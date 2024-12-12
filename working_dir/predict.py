import os
import numpy as np
from transformers import AutoModelForTokenClassification, AutoTokenizer
import torch

def load_model_and_tokenizer(model_dir):
    model = AutoModelForTokenClassification.from_pretrained(model_dir)
    tokenizer = AutoTokenizer.from_pretrained(model_dir)
    return model, tokenizer

def preprocess_input(text, tokenizer, max_length=128):
    tokenized_input = tokenizer(
        text,
        return_tensors="pt",  # PyTorch tensors
        padding="max_length",
        truncation=True,
        max_length=max_length
    )
    return tokenized_input

def predict_labels(model, tokenizer, text, id2label, max_length=128):
    # Preprocess the input text
    tokenized_input = preprocess_input(text, tokenizer, max_length)
    input_ids = tokenized_input["input_ids"]
    attention_mask = tokenized_input["attention_mask"]

    # Run the model to get predictions
    model.eval()  # Set model to evaluation mode
    with torch.no_grad():
        outputs = model(input_ids=input_ids, attention_mask=attention_mask)
        logits = outputs.logits
        predictions = np.argmax(logits.numpy(), axis=2)

    # Convert predictions to labels
    tokens = tokenizer.convert_ids_to_tokens(input_ids[0])
    predicted_labels = [id2label[label_id] for label_id in predictions[0]]

    # Post-process to merge subwords
    merged_tokens = []
    merged_labels = []
    current_token = ""
    current_label = None
    for token, label in zip(tokens, predicted_labels):
        if token.startswith("##"):
            current_token += token[2:]  # Remove "##" and append subword
        else:
            if current_token:  # Add the previous token and label
                merged_tokens.append(current_token)
                merged_labels.append(current_label)
            current_token = token
            current_label = label
    if current_token:  # Add the last token and label
        merged_tokens.append(current_token)
        merged_labels.append(current_label)

    return list(zip(merged_tokens, merged_labels))

if __name__ == "__main__":
    # Define paths
    model_dir = os.path.expanduser("~/.mozhi/models/hf/sroie2019v1")

    # Load the model and tokenizer
    model, tokenizer = load_model_and_tokenizer(model_dir)

    # Define the input text
    input_text = """Aa
<o
Perfect Vision Opticals

28, 1st Floor, 2nd Cross, 1st Block, RT Nagar, Mumbai-400050
GSTIN: 28AJHCU1234L1ZA PAN: RFRBG5397G

INVOICE

Invoice No: 39787 BILL TO

Invoice Date: 25-11-2021 Deepa Patel

P.O.No: 3888 73, Pine Boulevard

P.O.Date: 23-11-2021 Mangalore

Ref No: 5984 Pan No: EXTQG2481B

Ref Date: 25-11-2021 Mobile No: 9855902017
Email: deepapatel@gmail.com
GSTIN: 24GRZOB1843N1Z5

Description In Detail Discount

Polarized Wrap Around Sports 3479.00 9915.15
Sunglasses ,

Ray-Ban Aviator Classic 2957.00 5618.30
Sunglasses

Bobster Eyewear Fuel
Photochromic Goggles 2684.00 2549.80

TOTAL 18083.25

Sub Total 18083.25
IGST @ 18% 3254.99

Grand Total 21338.24
PAYMENT METHOD
we Accept the PayPal, Master Card, VISA and E- Wallets
Terms & Condition:

1. Goods Once Sold Will Not be Taken Back
2. Goods Can be Exchanged During 2:00 pm to 4:00 pm

Thank you for business & visit Again
"""

    # Predict labels
    id2label = model.config.id2label
    predictions = predict_labels(model, tokenizer, input_text, id2label)
    output={}
    # Display predictions
    for token, label in predictions:
        output[token]=label
    with open("output.txt","w") as f:
        for token, label in predictions:
            f.write(f"{token}\t{label}\n")
    print(output)
