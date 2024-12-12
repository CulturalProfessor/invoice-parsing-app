from evaluate import load
import datasets
from transformers import AutoTokenizer
from sroie2019_dataset import HFSREIO2019Dataset

metric = load("seqeval")
logger = datasets.logging.get_logger(__name__)

class HFTokenizer(object):
    NAME = "HFTokenizer"

    def __init__(self, hf_pretrained_tokenizer_checkpoint):
        self._tokenizer = AutoTokenizer.from_pretrained(hf_pretrained_tokenizer_checkpoint)

    @property
    def tokenizer(self):
        return self._tokenizer

    @staticmethod
    def init_vf(hf_pretrained_tokenizer_checkpoint):
        return HFTokenizer(hf_pretrained_tokenizer_checkpoint=hf_pretrained_tokenizer_checkpoint)

    def tokenize_and_align_labels(self, examples, label_all_tokens=True):
        tokenized_inputs = self._tokenizer(
            examples["tokens"],
            truncation=True,
            is_split_into_words=True
        )
        labels = []
        for i, label in enumerate(examples[f"ner_tags"]):
            word_ids = tokenized_inputs.word_ids(batch_index=i)
            previous_word_idx = None
            label_ids = []
            for word_idx in word_ids:
                if word_idx is None:
                    label_ids.append(-100)
                elif word_idx != previous_word_idx:
                    label_ids.append(label[word_idx])
                else:
                    label_ids.append(label[word_idx] if label_all_tokens else -100)
                previous_word_idx = word_idx
            labels.append(label_ids)
        tokenized_inputs["labels"] = labels
        return tokenized_inputs

if __name__ == '__main__':
    hf_pretrained_tokenizer_checkpoint = "distilbert-base-uncased"

    train_file = "./version1/train.txt"
    val_file = "./version1/valid.txt"
    test_file = "./version1/test.txt"

    # Initialize dataset with the file paths
    dataset = HFSREIO2019Dataset(
        train_file=train_file,
        val_file=val_file,
        test_file=test_file
    ).dataset

    # Initialize tokenizer
    hf_preprocessor = HFTokenizer.init_vf(
        hf_pretrained_tokenizer_checkpoint=hf_pretrained_tokenizer_checkpoint
    )

    # Apply tokenization and label alignment
    tokenized_datasets = dataset.map(hf_preprocessor.tokenize_and_align_labels, batched=True)

    print(dataset)
    print("*" * 100)
    print(tokenized_datasets)
    print("First sample: ", dataset['train'][0])
    print("*" * 100)
    print("First tokenized sample: ", tokenized_datasets['train'][0])
