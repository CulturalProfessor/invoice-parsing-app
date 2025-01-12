# 📄 Invoice Parsing App

## 🌟 Overview
The Invoice Parsing App is a mobile application designed to enhance the efficiency of invoice scanning and data extraction. Utilizing lightweight, on-device machine learning models, the app provides a robust offline tool for parsing and managing invoices. This ensures enhanced privacy and rapid inference without reliance on server infrastructure.

## ✨ Features

- **🔍 OCR-Based Scanning**: Extracts key information such as supplier details, itemized purchases, and taxes.
- **🤖 Hybrid Model Integration**:
  - **🛠️ ML Kit's Entity Extractor**: Identifies general fields like dates, addresses, and emails.
  - **📊 Fine-Tuned BiLSTM NER Model**: Extracts domain-specific entities such as invoice numbers and supplier names.
  - **📐 Proximity-Based Detection**: Recognizes entities like GST numbers and invoice IDs using spatial relationships.
- **✏️ Editable User Interface**: Users can verify and manually correct extracted data.
- **💾 Local Storage**: Parsed data is stored offline to enhance privacy and accessibility.

## ⚙️ Technical Highlights

### 🗂️ Data Preparation
- A dataset of 1,000 Tally invoices was collected and annotated.
- Regex patterns were employed to label entities such as dates, amounts, and invoice numbers.

### 🏋️‍♂️ Model Training
- A pre-trained BiLSTM NER model was fine-tuned for improved performance.
- The model was optimized for on-device usage using TensorFlow Lite (TFLite).

### 🔗 Integration
- Results from ML Kit and BiLSTM were combined into a unified system.
- Supported local Datastore DB for structured offline data management.

## ⚠️ Limitations

- **🌀 Complex Layouts**: The app struggles with unconventional invoice layouts.
- **📄 Preprocessing Dependency**: Requires high-quality scans for accurate extraction.
- **🤷‍♂️ Model Conflict Resolution**: Manual verification is necessary for conflicting outputs.
- **📊 Dataset Limitations**: Performance is tied to the quality of the training dataset.

## 🚀 Future Scope

- **🌐 Multi-Language Support**: Plans to expand capabilities to handle invoices in multiple languages.
- **📱 Cross-Platform Compatibility**: Extend support to iOS for broader accessibility.
- **📚 Batch Processing**: Enable simultaneous processing of multiple documents.
- **📑 Additional Document Types**: Incorporate support for PDFs, purchase orders, and contracts.

## 🛠️ Getting Started

### 🔧 Prerequisites

- 🖥️ Android Studio (for development and testing)
- 🐍 Python (for dataset preparation and model training)
- 🤖 TensorFlow Lite (for on-device model optimization)

### 📥 Installation

You can download the APK file for the Invoice Parsing App using the link below:

[📥 Download APK](https://drive.google.com/file/d/1B7jNNnXcQr640JWCQoIJcaJ1YMRtKzKE/view?usp=sharing)

Test app on this invoice:
[PerfectVisionInvoice_2024-07-08_18-33-16_45.pdf](https://github.com/user-attachments/files/18388248/PerfectVisionInvoice_2024-07-08_18-33-16_45.pdf)

Watch a demo of the app in action:

[▶️ Demo Video](https://youtube.com/shorts/ut8Lm09B75I?feature=share)

To get started with the Invoice Parsing App, clone the repository using the following command:

```bash
git clone https://github.com/CulturalProfessor/invoice-parsing-app.git
```
