/*
 * HDPS_PROTOTYPE.R
 *
 * Copyright 2010 Brigham and Women's Hospital. 
 *
 */

/*
	The contents of this file are subject to the Mozilla Public License Version 
	1.1 (the "License"); you may not use this file except in compliance with
	the License.  You may obtain a copy of the License at http://www.mozilla.org/MPL.
	
	The Original Code is the DoPE Pharmacoepidemiology Toolbox.
	
	The Initial Developer of the Original Code is the Brigham and Women's Hospital 
	Division of Pharmacoepidemiology.
	
	Contributor(s):
	   Jeremy A. Rassen <jrassen@post.harvard.edu>
 */

# The rJava library is required
library(rJava)

# initialize the Java subsystem.  include a jdbc object
# if desired
.jinit(classpath="/path/to/mysql-connector-java:/path/to/pharmacoepi.jar", parameters="-Xmx1g -Xms1g", force.init = TRUE);

# Instantiate an hd-PS object
hdps <- .jnew("org.drugepi.hdps.Hdps");

# Get and display the object version
version <- .jfield(hdps, "S", "version");
print(version);

# Set required hd-PS parameters
tempDir <- "/path/to/temp/directory";
.jfield(hdps, "tempDirectory") <- tempDir;

# Set optional hd-PS parameters
.jfield(hdps, "topN") <- as.integer(200);
.jfield(hdps, "k") <- as.integer(500);
.jfield(hdps, "doFullOutput") <- as.integer(1)
.jfield(hdps, "doSparseOutput") <- as.integer(1)

# Option 1: Specify input data as files
baseDir <- "/path/to/data/files/";
.jcall(hdps, "V", "addPatients", paste(baseDir, "patients.txt", sep=""));
.jcall(hdps, "V", "addDimension", "Dim 1", paste(baseDir, "dim1.txt", sep=""));
.jcall(hdps, "V", "addDimension", "Dim 2", paste(baseDir, "dim2.txt", sep=""));
.jcall(hdps, "V", "addDimension", "Dim 3", paste(baseDir, "dim3.txt", sep=""));

# Option 2: Specify input data as database queries
dbDriver <- "com.mysql.jdbc.Driver";
dbURL <- "jdbc:mysql://server:3306/database";
dbUser <- "user";
dbPassword <- "password";

.jcall(hdps, "V", "addPatients", dbDriver, dbURL, dbUser, dbPassword, 
				  "SELECT * FROM patients");

.jcall(hdps, "V", "addDimension", dbDriver, dbURL, dbUser, dbPassword, 
					"SELECT * FROM dim1");

# Run the hd-PS algorithm
.jcall(hdps, "V", "run");


# Read in the original patients file 
fileName <- paste(baseDir, "patients.txt", sep="");
patients <- read.table(fileName, header=TRUE, fill=TRUE);

# Read in the full cohort file
fileName <- paste(tempDir, "hdps_cohort.txt", sep="");
hdpsVars <- read.table(fileName, header=TRUE, fill=TRUE);

# Get the names of the selected variables; prepend hdpsVars$ to each
colNames <- colnames(hdpsVars)
colNames <- colNames[2:length(colNames)]
colNames <- paste("hdpsVars$", colNames, sep="")

# Create a formula that is expsosure ~ var1 + var 2 + ...
addedVars = paste(colNames, sep="", collapse="+")
fPs <- as.formula(paste("patients$exposure ~ ", addedVars, sep=""))

# Estimate the PS
ps <- glm(fPs, family="binomial")

# Get predicted values for each patient and create decile indicators
pspred <- predict(ps);
# !!! take expit of prpred
deciles <- cut(pspred, quantile(pspred, (0:10)/10), label=(1:10), 
			   include.lowest=TRUE)

# Estimate the treatment effect
fOutcome <- as.formula("patients$outcome ~ patients$exposure + deciles")
glm(fOutcome, family=binomial);
