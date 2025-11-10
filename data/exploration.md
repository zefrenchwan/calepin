# Data exploration

As a data engineer, data exploration sounds like a good way to get a clear understanding about what is coming in your data pipelines. 
For instance, how many lines, how many columns, what is the usual volume, etc. 
To do so, an option is **notebooks** to execute python code and explore data. 
The most famous one is Jupyter. 

## First steps

1. Create a virtual python environment to isolate it from the original installed python. 
2. `pip install ipykernel jupyter` to install jupyter notebook.  
3. Launch your code editor using that virtual env and run the notebook (on VS Code, it uses the venv. One may launch the notebook and then connect to it).  
4. First cell is about installing dependencies to use via a command `%pip install ...` to include all the dependencies. 


The idea was to use the venv to isolate dependencies, install just enough to launch the notebook and, within that notebook, import what is necessary for data exploration: 
* matplotlib: data visualization 
* pandas: dataframe manipulation (efficient until a few millions of rows)
* numpy: swiss knife for any data calculation
* fsspec and huggingface_hub: toolbox to use huggingface content (to be discussed later)

## Dealing with text and images 

Depending on what you do, but: 
* nltk and spacy are the most famous ones for language
* opencv is a classical one for computer vision