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
