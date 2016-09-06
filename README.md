# RDF4weka

A microservice that provides an interface to facilitate using RDF data and triplestores with weka. Using this microservice all you need is a query to get the data and a query to get the attributes and RDF4weka does the rest. The customizable queries are provided as environmental variables in the dockerfile. See the examples below to get started.
##JSON input:
Interaction with the microservice is done by sending REST requests to the backend, by default on localhost:80.

each JSON request needs two headers:

    Content-Type -> application/vnd.api+json
    Accept -> application/vnd.api+json
    
and a body which depends on the request.

##Examples
###Using FPGrowth for frequent itemset mining
To build a FPGrowth model, first edit the ASSOCIATOR_DATA_QUERY and ATTRIBUTE_QUERY.
Then use Postman / a frontend to make the following POST request:

POST localhost:80/build-model

    {
    "algorithm": "FPGROWTH",
    "options": ["-C","0.85","-M","0.028","-S"],
    "method": "RDFFile"
    }

This creates an FPGrowth model with the given options and writes it to an RDFFile. There are currently three storage options: triplestore, which writes the association rules to the triplestore defined in the dockerfile, native, which stores the model as a native weka file, and RDFFile, which stores it as a RDF file.

To use the newly created model send the following POST request:

POST localhost:80/determine-frequent-items

    {
    "attributes": ["1a3660c2-011c-4e96-9b1d-529afc305428","69f23426-9279-4fe6-a283-24c2aa4c855d"],
    "algorithm": "FPGROWTH",
    "identifier": "{{the id received from the previous request}}",
    "method": "native"
    }
    
This will load the model you just created into memory (in a memory-efficient way) and calculate 
the frequent items of the given attributes using the model. Note that the model only needs to be 
loaded once into memory, so next time you send a request with the same identifier but different 
attributes the calculation will be much faster.

