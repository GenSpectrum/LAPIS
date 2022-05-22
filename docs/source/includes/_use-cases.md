# Use Cases

We demonstrate an example for this API in R.

## Plot the global distribution of all sequences

```r
library(jsonlite)
library(ggplot2)

# Query the API
response <- fromJSON("https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated?fields=country")
data <- response$data

# Make a plot
ggplot(
  data,
  aes(x = "", y = count, fill = country)) + 
  geom_bar(width = 1, stat = "identity") + 
  coord_polar("y", start = 0) + 
  theme_minimal() + 
  theme(
    panel.grid=element_blank(),
    panel.border = element_blank(),
    axis.ticks = element_blank(),
    axis.title.x = element_blank(), 
    axis.title.y = element_blank(),
    axis.text.x = element_blank())
```
