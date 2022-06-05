Data versions
=============

Distinguishing data versions is an important technique in LAPIS to ensure consistency and correctness of down-stream analyses. It is not for versioning: apart from some cached values, LAPIS only maintains the most recent data and no older versions.


**Problem:**

Knowing the data version is relevant when you need the results from more than one request and need to ensure that the results are generated from the exact same underlying dataset.

Here is an example of a potential problem: Imagine you would like to obtain the proportion of sequences from Oceania in the whole dataset. To calculate it, you could make two API requests: the first to get the number of sequences from Oceania and the second for the total number of available sequences. However, an error arises if the dataset gets updated between the two requests and new sequences (including some from Oceania) are added. Then, when you divide the number from the first request by the number of the second request to obtain the proportion, the result would be too small and neither reflect the correct proportion in the previous dataset nor the proportion in the new dataset.


**Solution:**

Every server response contains the version number of the data. It is provided in the HTTP response header ``lapis-data-version``. For JSON responses, it is additionally provided in the ``dataVersion`` field. It is recommended to compare the data versions of the different responses. If they are not the same, the data should be re-fetched.

The data version is the Unix timestamp of the moment when the dataset was put in place.
