import os
import tensorflow as tf
from transformers import TFAutoModelForTokenClassification, AutoTokenizer

def convert_to_tflite(model_dir, tflite_model_path, tokenizer_path=None, max_length=128):
    """
    Convert a Hugging Face model to TFLite format with the correct input shape.

    Args:
        model_dir (str): Path to the Hugging Face SavedModel directory.
        tflite_model_path (str): Path to save the TFLite model.
        tokenizer_path (str): Path to the tokenizer. If None, uses model_dir.
        max_length (int): Maximum sequence length for the model.
    """
    print("Loading TensorFlow model...")
    model = TFAutoModelForTokenClassification.from_pretrained(model_dir,from_pt=True)

    # Define a tokenizer
    tokenizer = AutoTokenizer.from_pretrained(tokenizer_path or model_dir)

    # Create dummy input with the correct shape
    dummy_input = tokenizer(
        ["This is a dummy input for TFLite conversion."],
        return_tensors="tf",
        padding="max_length",
        max_length=max_length,
    )
    input_ids = tf.constant(dummy_input["input_ids"], dtype=tf.int32)
    attention_mask = tf.constant(dummy_input["attention_mask"], dtype=tf.int32)

    # Define a function to serve as input signature for TFLite conversion
    def model_serving(input_ids, attention_mask):
        return model(input_ids=input_ids, attention_mask=attention_mask)

    # Convert the model to TFLite format
    print("Converting to TFLite format...")
    converter = tf.lite.TFLiteConverter.from_concrete_functions(
        [tf.function(model_serving).get_concrete_function(
            input_ids=tf.TensorSpec(shape=[1, max_length], dtype=tf.int32),
            attention_mask=tf.TensorSpec(shape=[1, max_length], dtype=tf.int32),
        )]
    )
    # converter.optimizations = [tf.lite.Optimize.DEFAULT]  # Enable optimization (optional)
    tflite_model = converter.convert()

    # Save the TFLite model
    print(f"Saving TFLite model to {tflite_model_path}...")
    with open(tflite_model_path, "wb") as f:
        f.write(tflite_model)

    print("Conversion completed successfully!")


if __name__ == "__main__":
    # Define paths
    model_dir = os.path.expanduser("~/.mozhi/models/hf/sroie2019v1")  # Path to your Hugging Face model
    tflite_model_path = "./sroie2019v1.tflite"  # Path to save the TFLite model
    tokenizer_path = None  # Optional: specify a custom tokenizer path
    max_length = 128  # Set the maximum sequence length

    # Convert the model
    convert_to_tflite(model_dir, tflite_model_path, tokenizer_path, max_length)

    print("TFLite model saved!")
