import pandas as pd
import os
from tqdm import tqdm
from tags import custom_tags
def load_tagged_csv(file_path):
    """
    Load tagged data from a CSV file and convert it to CoNLL format.

    Args:
        file_path (str): Path to the tagged CSV file.

    Returns:
        list: CoNLL formatted data.
    """
    data = pd.read_csv(file_path)
    conll_data = []
    current_filename = None

    for _, row in data.iterrows():
        filename = row['Filename']
        token = row['Token']
        tag = row['Tag']

        # Add a blank line to separate sentences if the file changes
        if current_filename and current_filename != filename:
            conll_data.append(("", "O"))
        current_filename = filename

        # Append the token and its tag
        conll_data.append((token, tag))

    conll_data.append(("", "O"))  # Ensure the final sentence is separated
    return conll_data

def validate_tags(data, custom_tags):
    """
    Validate that all tags in the data are part of the custom tags.

    Args:
        data (list): CoNLL formatted data.
        custom_tags (list): List of allowed tags.

    Raises:
        ValueError: If an invalid tag is found.
    """
    for _, tag in data:
        if tag not in custom_tags:
            raise ValueError(f"Invalid tag detected: {tag}")

def split_data(data, train_ratio=0.8, valid_ratio=0.1):
    """
    Split the data into train, validation, and test sets.

    Args:
        data (list): CoNLL formatted data.
        train_ratio (float): Proportion of the data to use for training.
        valid_ratio (float): Proportion of the data to use for validation.

    Returns:
        tuple: Train, validation, and test datasets.
    """
    total_len = len(data)
    train_end = int(total_len * train_ratio)
    valid_end = int(total_len * (train_ratio + valid_ratio))

    train_data = data[:train_end]
    valid_data = data[train_end:valid_end]
    test_data = data[valid_end:]

    return train_data, valid_data, test_data

def write_conll_file(file_path, data):
    """
    Write CoNLL formatted data to a file.

    Args:
        file_path (str): Path to the output file.
        data (list): CoNLL formatted data.
    """
    with open(file_path, "w") as file:
        for token, tag in data:
            if token:
                file.write(f"{token} {tag}\n")
            else:
                file.write("\n")

def save_as_dataframe(file_path, data):
    """
    Save CoNLL data to a CSV file.
    
    Args:
        file_path (str): Path to the output CSV file.
        data (list): CoNLL formatted data.
    """
    df = pd.DataFrame(data, columns=["tokens", "ner_tags"])
    df.to_csv(file_path, index=False)

def preprocess_csv_for_tokenizer(csv_path):
    """
    Preprocess a CSV of tokens and ner_tags into nested sequences.

    Args:
        csv_path (str): Path to the CSV file.

    Returns:
        list: A list of dictionaries with 'tokens' and 'ner_tags'.
    """
    df = pd.read_csv(csv_path)
    sequences = []

    for _, row in df.iterrows():
        # Skip rows with NaN or missing values
        if pd.isna(row["tokens"]) or pd.isna(row["ner_tags"]):
            continue

        try:
            tokens = row["tokens"].strip("[]").replace("'", "").split(", ")
            ner_tags = row["ner_tags"].strip("[]").replace("'", "").split(", ")
        except AttributeError:
            raise ValueError(f"Invalid row format: {row}")

        if len(tokens) != len(ner_tags):
            raise ValueError(f"Token and tag counts do not match: {row}")

        sequences.append({"tokens": tokens, "ner_tags": ner_tags})

    print(f"Processed {len(sequences)} sequences.")
    return sequences

if __name__ == "__main__":
    # Load and preprocess the flat tagged data
    print("Preprocessing CSV into nested sequences...")
    sequences = preprocess_csv_for_tokenizer("./tagged_output.csv")

    # Create a CoNLL format from the nested sequences
    print("Converting sequences to CoNLL format...")
    conll_data = []
    for sequence in sequences:
        tokens = sequence["tokens"]
        tags = sequence["ner_tags"]
        for token, tag in zip(tokens, tags):
            conll_data.append((token, tag))

    # Validate tags
    print("Validating tags...")
    validate_tags(conll_data, custom_tags)

    # Split the data into train, validation, and test sets
    print("Splitting data...")
    train_data, valid_data, test_data = split_data(conll_data)

    # Output directory
    output_dir = "./version1"
    os.makedirs(output_dir, exist_ok=True)

    # Save as CoNLL-style text files
    print("Saving datasets as CoNLL text files...")
    write_conll_file(f"{output_dir}/train.txt", train_data)
    write_conll_file(f"{output_dir}/valid.txt", valid_data)
    write_conll_file(f"{output_dir}/test.txt", test_data)

    print("Dataset preparation completed!")
