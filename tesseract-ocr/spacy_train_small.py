import csv
import re
import json
import random
from tags import custom_tags
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

# Define patterns
pan_pattern = re.compile(r"^[A-Z]{5}\d{4}[A-Z]{1}$")
phone_pattern = re.compile(r"^(\+91[\-\s]?)?[0]?[789]\d{9}$")
pincode_pattern = re.compile(r"^\d{6}$")


def is_indian_name(token):
    return token in common_indian_first_names or token in common_indian_surnames


def is_optical_item_token(token):
    return token.lower() in optical_brands or token.lower() in optical_keywords


def truncate_text(text, max_tokens=50):
    tokens = text.split()
    return " ".join(tokens[:max_tokens])

def tag_sections_with_offsets(text):
    tokens = re.finditer(r"\S+", text)  # Match tokens with whitespace as delimiter
    entities = []
    in_address_block = False  # Tracks if the current token is part of an address block
    previous_tag = None  # Tracks the previous tag for contextual tagging

    for match in tokens:
        token = match.group()
        start = match.start()
        end = match.end()

        tag = None  # Default to None for untagged tokens

        # Check for known patterns
        if re.match(r"^\d{7,}$", token):  # Invoice number (digits > 6)
            tag = "B-INVOICE_NUMBER"
            in_address_block = False
        elif (
            re.match(r"^\d{2}/\d{2}/\d{4}$", token)
            or re.match(r"^\d{4}-\d{2}-\d{2}$", token)
            or re.match(r"^\d{2}-\d{2}-\d{4}$", token)
        ):  # Date formats
            tag = "B-INVOICE_DATE"
            in_address_block = False
        elif re.match(r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]$", token):  # GSTIN
            tag = "B-GSTIN"
            in_address_block = False
        elif pan_pattern.match(token):  # PAN
            tag = "B-PAN"
            in_address_block = False
        elif phone_pattern.match(token):  # Phone number
            tag = "B-CUSTOMER_PHONE"
            in_address_block = False
        elif re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", token):  # Email
            tag = "B-CUSTOMER_EMAIL"
            in_address_block = False
        elif (
            token in indian_states
            or token in major_cities
            or pincode_pattern.match(token)
            or token.lower() in address_keywords
        ):  # Address detection
            tag = "B-CUSTOMER_ADDRESS"
            in_address_block = True
        elif re.match(r"^\d+$", token):  # Quantity
            tag = "B-QUANTITY"
            in_address_block = False
        elif re.match(r"(subtotal|tax|total|amount|balance|due)", token, re.IGNORECASE):  # Financial terms
            tag = "B-SUBTOTAL"
            in_address_block = False
        elif re.match(r"^\d+(\.\d{1,2})?$", token):  # Amount
            tag = "B-AMOUNT"
            in_address_block = False
        elif token.lower() in optical_brands:  # Optical brands
            tag = "B-ITEM_DESCRIPTION"
            in_address_block = False
        elif is_optical_item_token(token):  # Optical-related keywords
            tag = "B-ITEM_DESCRIPTION"
            in_address_block = False
        elif token[0].isupper() and token not in known_invoice_terms:
            if in_address_block and previous_tag in ["B-CUSTOMER_ADDRESS", "B-CUSTOMER_NAME"]:  # Likely a name in address block
                tag = "B-CUSTOMER_NAME"
            elif previous_tag in ["B-INVOICE_DATE", "B-VENDOR_NAME"]:  # Contextual vendor tagging
                tag = "B-VENDOR_NAME"
            elif previous_tag in ["B-CUSTOMER_ADDRESS", "B-CUSTOMER_PHONE"]:  # Contextual customer tagging
                tag = "B-CUSTOMER_NAME"
            else:  # Default fallback for uppercase tokens
                tag = "O"
        else:
            in_address_block = False  # Reset address block for unknown tokens

        # Add tagged entity if valid
        if tag and tag in custom_tags:
            entities.append([start, end, tag])
            previous_tag = tag

    return {"text": text, "entities": entities}


def generate_json_training_data(input_csv, max_rows=None):
    training_data = []
    with open(input_csv, "r", encoding="utf-8") as infile:
        reader = list(csv.DictReader(infile))
        if "Extracted Text" not in reader[0]:
            raise ValueError(f"Missing 'Extracted Text' column in {input_csv}")

        # Sample rows randomly if specified
        if max_rows:
            reader = random.sample(reader, min(max_rows, len(reader)))

        for i, row in enumerate(reader):
            if max_rows and i >= max_rows:
                break
            extracted_text = row.get("Extracted Text", "")
            if not extracted_text.strip():
                continue
            processed_data = tag_sections_with_offsets(extracted_text)
            training_data.append(
                [processed_data["text"], {"entities": processed_data["entities"]}]
            )
    return training_data


if __name__ == "__main__":
    input_csv_paths = [
        "./extracted_texts.csv",
        "./extracted_data/extracted_text_2024-11-25 00:44:32.590724.csv",
    ]
    output_json_path = "./training_data.json"

    training_data = []
    max_rows_per_file = 10
    random_sampling = True

    for input_csv_path in input_csv_paths:
        training_data.extend(
            generate_json_training_data(
                input_csv_path,
                max_rows=max_rows_per_file,
            )
        )

    with open(output_json_path, "w", encoding="utf-8") as outfile:
        json.dump(
            {"classes": custom_tags, "annotations": training_data},
            outfile,
            indent=4,
            ensure_ascii=False,
        )

    print(f"Reduced training data saved to {output_json_path}")
