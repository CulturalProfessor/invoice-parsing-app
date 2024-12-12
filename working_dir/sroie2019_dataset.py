# Ref: https://github.com/huggingface/datasets/blob/master/datasets/conll2003/conll2003.py

import datasets
import os
from pathlib import Path
from datasets import ClassLabel, DownloadConfig

logger = datasets.logging.get_logger(__name__)


_CITATION = ""
_DESCRIPTION = """\

"""


class SROIE2019Config(datasets.BuilderConfig):
    """BuilderConfig for SROIE2019"""

    def __init__(self, **kwargs):
        """BuilderConfig for SROIE2019.

        Args:
          **kwargs: keyword arguments forwarded to super.
        """
        super(SROIE2019Config, self).__init__(**kwargs)


class SROIE2019(datasets.GeneratorBasedBuilder):
    """SROIE2019 dataset."""

    BUILDER_CONFIGS = [
        SROIE2019Config(name="SROIE2019", version=datasets.Version("1.0.0"), description="SROIE2019 dataset"),
    ]

    def __init__(self,
                 *args,
                #  cache_dir,
                 train_file="./version1/train.txt",
                 val_file="./version1/valid.txt",
                 test_file="./version1/test.txt",
                 ner_tags=(    "O", "B-INVOICE_NUMBER", "I-INVOICE_NUMBER",
    "B-INVOICE_DATE", "I-INVOICE_DATE", "B-DUE_DATE", "I-DUE_DATE",
    "B-CUSTOMER_PO", "I-CUSTOMER_PO", "B-VENDOR_NAME", "I-VENDOR_NAME",
    "B-VENDOR_ADDRESS", "I-VENDOR_ADDRESS", "B-VENDOR_PHONE", "I-VENDOR_PHONE",
    "B-VENDOR_EMAIL", "I-VENDOR_EMAIL", "B-VENDOR_WEBSITE", "I-VENDOR_WEBSITE",
    "B-CUSTOMER_NAME", "I-CUSTOMER_NAME", "B-CUSTOMER_ADDRESS", "I-CUSTOMER_ADDRESS",
    "B-CUSTOMER_PHONE", "I-CUSTOMER_PHONE", "B-CUSTOMER_EMAIL", "I-CUSTOMER_EMAIL",
    "B-ITEM_DESCRIPTION", "I-ITEM_DESCRIPTION", "B-QUANTITY", "B-UNIT_PRICE",
    "B-TOTAL_PRICE", "B-SUBTOTAL", "I-SUBTOTAL", "B-TAX_AMOUNT", "I-TAX_AMOUNT",
    "B-TOTAL_AMOUNT_DUE", "I-TOTAL_AMOUNT_DUE", "B-PAYMENT_TERMS", "I-PAYMENT_TERMS",
    "B-PAYMENT_METHOD", "I-PAYMENT_METHOD", "B-BANK_DETAILS", "I-BANK_DETAILS",
    "B-NOTES", "I-NOTES", "B-GST", "B-GSTIN", "I-GSTIN", "B-CGST", "B-SGST", "B-IGST",
    "B-AMOUNT", "I-AMOUNT", "B-SUB-TOTAL", "I-SUB-TOTAL"),
                 **kwargs):
        self._ner_tags = ner_tags
        self._train_file = train_file
        self._val_file = val_file
        self._test_file = test_file
        super(SROIE2019, self).__init__(*args, **kwargs)

    def _info(self):
        return datasets.DatasetInfo(
            description=_DESCRIPTION,
            features=datasets.Features(
                {
                    "id": datasets.Value("string"),
                    "tokens": datasets.Sequence(datasets.Value("string")),
                    "ner_tags": datasets.Sequence(
                        datasets.features.ClassLabel(
                            names=sorted(list(self._ner_tags))
                        )
                    )
                }
            ),
            supervised_keys=None,
            homepage="",
            citation=_CITATION,
        )

    def _split_generators(self, dl_manager):
        """Returns SplitGenerators."""
        # Use local paths for dataset files
        local_files = {
            "train": os.path.abspath(self._train_file),
            "dev": os.path.abspath(self._val_file),
            "test": os.path.abspath(self._test_file),
        }

        return [
            datasets.SplitGenerator(name=datasets.Split.TRAIN, gen_kwargs={"filepath": local_files["train"]}),
            datasets.SplitGenerator(name=datasets.Split.VALIDATION, gen_kwargs={"filepath": local_files["dev"]}),
            datasets.SplitGenerator(name=datasets.Split.TEST, gen_kwargs={"filepath": local_files["test"]}),
        ]

    def _generate_examples(self, filepath):
        logger.info("⏳ Generating examples from = %s", filepath)
        with open(filepath, encoding="utf-8") as f:
            guid = 0
            tokens = []
            ner_tags = []
            for line in f:
                if line.strip() == "":
                    if tokens:
                        yield guid, {
                            "id": str(guid),
                            "tokens": tokens,
                            "ner_tags": ner_tags,
                        }
                        guid += 1
                        tokens = []
                        ner_tags = []
                else:
                    splits = line.split()
                    tokens.append(splits[0])
                    ner_tags.append(splits[1].rstrip())
            # Yield last example if any
            if tokens:
                yield guid, {
                    "id": str(guid),
                    "tokens": tokens,
                    "ner_tags": ner_tags,
                }

class HFSREIO2019Dataset(object):
    """
    Dataset class for SROIE2019.
    """
    NAME = "HFSREIO2019Dataset"

    def __init__(self, train_file, val_file, test_file):
        self._train_file = train_file
        self._val_file = val_file
        self._test_file = test_file

        # Initialize dataset
        self._dataset = SROIE2019(
            train_file=self._train_file,
            val_file=self._val_file,
            test_file=self._test_file
        )
        # Prepare dataset directly without caching
        self._dataset.download_and_prepare()
        self._dataset = self._dataset.as_dataset()

    @property
    def dataset(self):
        return self._dataset

    @property
    def labels(self):
        return self._dataset['train'].features['ner_tags'].feature.names

    @property
    def id2label(self):
        return dict(enumerate(self.labels))

    @property
    def label2id(self):
        return {v: k for k, v in self.id2label.items()}

    def train(self):
        return self._dataset['train']

    def test(self):
        return self._dataset["test"]

    def validation(self):
        return self._dataset["validation"]

if __name__ == '__main__':
    # Update with paths to your local files
    train_file = "./version1/train.txt"
    val_file = "./version1/valid.txt"
    test_file = "./version1/test.txt"

    dataset = HFSREIO2019Dataset(train_file, val_file, test_file).dataset

    print(dataset['train'])
    print(dataset['test'])
    print(dataset['validation'])

    print("List of tags: ", dataset['train'].features['ner_tags'].feature.names)
    print("First sample: ", dataset['train'][0])