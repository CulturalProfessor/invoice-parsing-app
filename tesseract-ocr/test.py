import pandas as pd

# Load the tagged data
file_path = './tagged_output.csv'
data = pd.read_csv(file_path)

# Extract unique tags
unique_tags = data['Tag'].unique()

# Display the unique tags
print("Unique Tags:")
print(unique_tags.size)

# count occurences of each tag
tag_counts = data['Tag'].value_counts()
print(tag_counts)


