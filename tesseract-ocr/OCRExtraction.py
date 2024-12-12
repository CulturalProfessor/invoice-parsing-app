import datetime
import os
import csv
import pytesseract
from pdf2image import convert_from_path

def extract_text_from_pdf(pdf_path, output_dir):
    images = convert_from_path(pdf_path, dpi=300, output_folder=output_dir, fmt="png")

    extracted_text = []
    for i, image in enumerate(images):
        image_path = os.path.join(output_dir, f"page_{i + 1}.png")
        image.save(image_path)

        text = pytesseract.image_to_string(image, lang="eng")
        extracted_text.append(text)

    return "\n".join(extracted_text)


def process_pdfs_to_csv(input_dir, output_dir, output_csv_path):
    os.makedirs(output_dir, exist_ok=True)

    with open(output_csv_path, mode="w", encoding="utf-8", newline="") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["Filename", "Extracted Text"])

        for pdf_file in os.listdir(input_dir):
            if pdf_file.lower().endswith(".pdf"):
                pdf_path = os.path.join(input_dir, pdf_file)
                print(f"Processing {pdf_file}...")

                pdf_output_dir = os.path.join(output_dir, os.path.splitext(pdf_file)[0])
                os.makedirs(pdf_output_dir, exist_ok=True)

                try:
                    text = extract_text_from_pdf(pdf_path, pdf_output_dir)
                    writer.writerow([pdf_file, text]) 
                    print(f"Text extracted and saved for {pdf_file}")
                except Exception as e:
                    print(f"Failed to process {pdf_file}: {e}")


if __name__ == "__main__":
    input_pdf_dir = "./downloads/Reciept"
    output_dir = "./extracted"
    current_time = datetime.datetime.now()
    output_csv_path = f"./extracted_data/extracted_text_{current_time}.csv"

    process_pdfs_to_csv(input_pdf_dir, output_dir, output_csv_path)
