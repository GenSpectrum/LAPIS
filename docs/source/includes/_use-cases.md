# Use Cases

We demonstrate two use cases for this API.


## Create a XXX Plot

```r
library(jsonlite)

response <- fromJSON("https://cov-spectrum.ethz.ch/public/api/v1/sample/aggregated?fields=nextstrainClade,country")

errors <- response$errors
if (length(errors) > 0) {
  stop("Errors")
}

deprecationDate <- response$info$deprecationDate
if (!is.null(deprecationDate)) {
  warning(paste0("This version of the API will be deprecated on ", deprecationDate,
                 ". Message: ", response$info$deprecationInfo))
}


data <- response$payload


# Create a small plot :)
```

```python
data = download_from_web()
df = parse(data)
plot = matplotlib(df)
```

Steps:

1. Fetch data from API
2. Parse JSON
3. Check whether there are errors. If yes, abort!
4. Check whether a deprecation date is given. If yes, write a warning.
5. Transform the JSON to a data frame.
6. Use the data frame to create a plot.


## Notification system that sends a daily email with the number of VOCs in a selected country


