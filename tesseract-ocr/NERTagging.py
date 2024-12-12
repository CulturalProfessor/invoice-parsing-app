import csv
import re
import pandas as pd
from tags import custom_tags
from collections import Counter

# Import lexicons from a separate file if desired, or define them here
from lexicons import (
    known_invoice_terms,
    common_indian_first_names,
    common_indian_surnames,
    optical_brands,
    optical_keywords,
    indian_states,
    major_cities,
    address_keywords,
)

unknown_tags = {}

pan_pattern = re.compile(r"^[A-Z]{5}\d{4}[A-Z]{1}$")
phone_pattern = re.compile(r"^(\+91[\-\s]?)?[0]?[789]\d{9}$")
pincode_pattern = re.compile(r"^\d{6}$")

def is_indian_name(token):
    return token in common_indian_first_names or token in common_indian_surnames

def is_optical_item_token(token):
    return token.lower() in optical_brands or token.lower() in optical_keywords

def tag_sections(tokens):
    tagged_tokens = []
    previous_tag = "O"

    for i, token in enumerate(tokens):
        clean_token = token.strip(",.")
        if not clean_token:
            tagged_tokens.append({"tokens": clean_token, "ner_tags": "O"})
            continue

        tag = "O"

        # Check for known patterns
        if re.match(r"^\d{7,}$", clean_token):
            tag = "B-INVOICE"
        elif (
            re.match(r"^\d{2}/\d{2}/\d{4}$", clean_token)
            or re.match(r"^\d{4}-\d{2}-\d{2}$", clean_token)
            or re.match(r"^\d{2}-\d{2}-\d{4}$", clean_token)
        ):
            tag = "B-DATE"
        elif re.match(r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]$", clean_token):
            tag = "B-GST"
        elif pan_pattern.match(clean_token):
            tag = "B-PAN"
        elif phone_pattern.match(clean_token):
            tag = "B-PHONE"
        elif re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", clean_token):
            tag = "B-EMAIL"
        elif (
            clean_token in indian_states
            or clean_token in major_cities
            or pincode_pattern.match(clean_token)
        ):
            tag = (
                "B-ADDRESS"
                if previous_tag not in ["I-ADDRESS", "B-ADDRESS"]
                else "I-ADDRESS"
            )
        elif clean_token.lower() in address_keywords:
            tag = (
                "B-ADDRESS"
                if previous_tag not in ["I-ADDRESS", "B-ADDRESS"]
                else "I-ADDRESS"
            )
        elif re.match(r"^\d+$", clean_token):
            tag = "B-QUANTITY"
        elif re.match(
            r"(subtotal|tax|total|amount|balance|due)", clean_token, re.IGNORECASE
        ):
            tag = (
                "B-TAX"
                if previous_tag == "O"
                else "I-TAX"
            )
        elif re.match(r"^\d+(\.\d{1,2})?$", clean_token):
            tag = "B-PRICE" if previous_tag == "O" else "I-PRICE"
        else:
            if (
                clean_token[0].isupper()
                and clean_token not in known_invoice_terms
                and is_indian_name(clean_token)
            ):
                tag = "B-CUSTOMER" if previous_tag in ["O"] else "I-CUSTOMER"
            else:
                if clean_token[0].isupper() and clean_token not in known_invoice_terms:
                    tag = "B-VENDOR" if previous_tag in ["O"] else "I-VENDOR"
                elif is_optical_item_token(clean_token):
                    tag = (
                        "B-ITEM"
                        if previous_tag in ["O"]
                        else "I-ITEM"
                    )

        if tag not in custom_tags:
            unknown_tags[clean_token] = tag
            tag = "O"

        tagged_tokens.append({"tokens": clean_token, "ner_tags": tag})
        previous_tag = tag
    return tagged_tokens

def process_invoice_text(text, filename="unknown_file"):
    tokens = text.split()
    return tag_sections(tokens)

def generate_tagged_csv(input_csv, output_csv):
    with open(input_csv, "r", encoding="utf-8") as infile, open(
        output_csv, "a", encoding="utf-8", newline=""
    ) as outfile:
        reader = csv.DictReader(infile)
        fieldnames = ["filename", "tokens", "ner_tags"]
        writer = csv.DictWriter(outfile, fieldnames=fieldnames)
        if outfile.tell() == 0:
            writer.writeheader()
        for row in reader:
            filename = row.get("Filename", "unknown_file")
            extracted_text = row.get("Extracted Text", "")
            if not extracted_text.strip():
                continue
            tagged_data = process_invoice_text(extracted_text, filename=filename)
            for tagged_entry in tagged_data:
                writer.writerow(
                    {
                        "filename": filename,
                        "tokens": tagged_entry["tokens"],
                        "ner_tags": tagged_entry["ner_tags"],
                    }
                )

def preprocess_csv(csv_path):
    df = pd.read_csv(csv_path)
    required_columns = {"tokens", "ner_tags"}
    if not required_columns.issubset(df.columns):
        raise ValueError(
            f"CSV file must contain the following columns: {required_columns}"
        )
    sequences = []
    tokens, tags = [], []
    for _, row in df.iterrows():
        if pd.isna(row["tokens"]) or pd.isna(row["ner_tags"]):
            if tokens:
                sequences.append({"tokens": tokens, "ner_tags": tags})
                tokens, tags = [], []
        else:
            tokens.append(row["tokens"])
            tags.append(row["ner_tags"])
    if tokens:
        sequences.append({"tokens": tokens, "ner_tags": tags})
    print(f"Preprocessed {len(sequences)} sequences from {csv_path}")
    return sequences

def process():
    input_csv_paths = [
        "./extracted_texts.csv",
        "./extracted_data/extracted_text_2024-11-25 00:44:32.590724.csv",
    ]
    output_csv_path = "./tagged_output.csv"
    with open(output_csv_path, "w", encoding="utf-8", newline="") as outfile:
        outfile.write("")
    for csv_path in input_csv_paths:
        generate_tagged_csv(csv_path, output_csv_path)
    print("\nUnknown Tags Summary:")
    for token, tag in unknown_tags.items():
        print(f"Token: '{token}', Tag: '{tag}'")
    print(f"Tagged data saved to {output_csv_path}")

if __name__ == "__main__":
    process()
    csv_path = "./tagged_output.csv"
    processed_data = preprocess_csv(csv_path)
    from collections import Counter

    tag_counts = Counter()
    for seq in processed_data:
        for t in seq["ner_tags"]:
            tag_counts[t] += 1
    print("Tag Counts:")
    for tag, count in tag_counts.items():
        print(f"{tag}: {count}")
