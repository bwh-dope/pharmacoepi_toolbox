pharmacoepi_toolbox
===================

The core toolbox is written in Java and can be used directly that way. Alternatively, sample source code is provided to call the Java library from R and SAS.

## Data Format
Data should be in tab separated columns. For SAS, the data is transferred from SAS format automatically, for R, you have to prepare it yourself.

## Missing values
Although there is a way to handle missing values (???), actual *missing* values will cause the software to crash. So either code them properly (???) or delete rows with missing values in advance.
