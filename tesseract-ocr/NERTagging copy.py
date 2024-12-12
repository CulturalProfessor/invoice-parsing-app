import csv
import re
import pandas as pd
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
custom_tags = [
    "O", 
    "B-INVOICE", "I-INVOICE",
    "B-DATE", "I-DATE",
    "B-PO", "I-PO",
    "B-VENDOR", "I-VENDOR",
    "B-CUSTOMER", "I-CUSTOMER",
    "B-ADDRESS", "I-ADDRESS",
    "B-PHONE", "I-PHONE",
    "B-EMAIL", "I-EMAIL",
    "B-WEBSITE", "I-WEBSITE",
    "B-ITEM", "I-ITEM",
    "B-QUANTITY", "I-QUANTITY",
    "B-PRICE", "I-PRICE",
    "B-SUBTOTAL", "I-SUBTOTAL",
    "B-TAX", "I-TAX",
    "B-TOTAL", "I-TOTAL",
    "B-PAYMENT", "I-PAYMENT",
    "B-BANK", "I-BANK",
    "B-NOTES", "I-NOTES",
    "B-GST", "I-GST",
    "B-TAX-COMPONENT", "I-TAX-COMPONENT"
]

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
            tag = "B-INVOICE_NUMBER"
        elif (
            re.match(r"^\d{2}/\d{2}/\d{4}$", clean_token)
            or re.match(r"^\d{4}-\d{2}-\d{2}$", clean_token)
            or re.match(r"^\d{2}-\d{2}-\d{4}$", clean_token)
        ):
            tag = "B-INVOICE_DATE"
        elif re.match(r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]$", clean_token):
            tag = "B-GSTIN"
        elif pan_pattern.match(clean_token):
            tag = "B-PAN"
        elif phone_pattern.match(clean_token):
            tag = "B-CUSTOMER_PHONE"
        elif re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", clean_token):
            tag = "B-CUSTOMER_EMAIL"
        elif (
            clean_token in indian_states
            or clean_token in major_cities
            or pincode_pattern.match(clean_token)
        ):
            addr_tag = "CUSTOMER_ADDRESS"
            tag = (
                f"B-{addr_tag}"
                if previous_tag not in [f"I-{addr_tag}", f"B-{addr_tag}"]
                else f"I-{addr_tag}"
            )
        elif clean_token.lower() in address_keywords:
            addr_tag = "CUSTOMER_ADDRESS"
            tag = (
                f"B-{addr_tag}"
                if previous_tag not in [f"I-{addr_tag}", f"B-{addr_tag}"]
                else f"I-{addr_tag}"
            )
        elif re.match(r"^\d+$", clean_token):
            tag = "B-QUANTITY"
        elif re.match(
            r"(subtotal|tax|total|amount|balance|due)", clean_token, re.IGNORECASE
        ):
            uppercase_token = clean_token.upper()
            tag = (
                f"B-{uppercase_token}"
                if previous_tag == "O"
                else f"I-{uppercase_token}"
            )
        elif re.match(r"^\d+(\.\d{1,2})?$", clean_token):
            tag = "B-AMOUNT" if previous_tag == "O" else "I-AMOUNT"
        else:
            if (
                clean_token[0].isupper()
                and clean_token not in known_invoice_terms
                and is_indian_name(clean_token)
            ):
                tag = "B-CUSTOMER_NAME" if previous_tag in ["O"] else "I-CUSTOMER_NAME"
            else:
                if clean_token[0].isupper() and clean_token not in known_invoice_terms:
                    tag = "B-VENDOR_NAME" if previous_tag in ["O"] else "I-VENDOR_NAME"
                elif is_optical_item_token(clean_token):
                    tag = (
                        "B-ITEM_DESCRIPTION"
                        if previous_tag in ["O"]
                        else "I-ITEM_DESCRIPTION"
                    )

        if tag not in custom_tags:
            unknown_tags[clean_token] = tag
            tag = "O"

        tagged_tokens.append({"tokens": clean_token, "ner_tags": tag})
        previous_tag = tag
    return tagged_tokens

def process_invoice_text(text, filename="unknown_file"):
    sentences = re.split(r"(?<!\w\.\w.)(?<![A-Z][a-z]\.)(?<=\.|\?)\s", text)
    processed_sentences = []
    for sentence in sentences:
        tokens = sentence.split()
        tagged_tokens = tag_sections(tokens)
        processed_sentences.append({
            "sentence": sentence,
            "tokens": [tagged["tokens"] for tagged in tagged_tokens],
            "ner_tags": [tagged["ner_tags"] for tagged in tagged_tokens],
        })
    return processed_sentences

def generate_tagged_csv(input_csv, output_csv):
    with open(input_csv, "r", encoding="utf-8") as infile, open(
        output_csv, "a", encoding="utf-8", newline=""
    ) as outfile:
        reader = csv.DictReader(infile)
        fieldnames = ["filename", "sentence", "tokens", "ner_tags"]
        writer = csv.DictWriter(outfile, fieldnames=fieldnames)
        if outfile.tell() == 0:
            writer.writeheader()
        for row in reader:
            filename = row.get("Filename", "unknown_file")
            extracted_text = row.get("Extracted Text", "")
            if not extracted_text.strip():
                continue
            processed_data = process_invoice_text(extracted_text, filename=filename)
            for sentence_data in processed_data:
                writer.writerow(
                    {
                        "filename": filename,
                        "sentence": sentence_data["sentence"],
                        "tokens": " ".join(sentence_data["tokens"]),
                        "ner_tags": " ".join(sentence_data["ner_tags"]),
                    }
                )

def process():
    input_csv_paths = [
        "./extracted_texts.csv",
        "./extracted_data/extracted_text_2024-11-25 00:44:32.590724.csv",
    ]
    output_csv_path = "./tagged_output_lstm.csv"
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
    processed_data = pd.read_csv(csv_path)
    print(processed_data.head())
