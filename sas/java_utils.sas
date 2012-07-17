/*
 * JAVA_UTILS.SAS
 *
 * Copyright 2010 Brigham and Women's Hospital.  
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

/*
Initialize updating the Java classpath.
*/
%macro javautils_InitClasspathUpdate;
	DATA _null_;
	    LENGTH  path_separator $ 2
	            orig_classpath $ 500;

	    DECLARE JavaObj f("java.io.File", "");
	    f.getStaticStringField("pathSeparator", path_separator);

	    orig_classpath = STRIP(SYSGET("CLASSPATH"));

	    IF _ERROR_ = 1 OR LENGTH(orig_classpath) = 0 THEN DO;
			PUT "NOTE: Due to SAS limitations, ignore any classpath-related messages from the prior or next statement(s)";
	        orig_classpath = "";
		END;

	    CALL SYMPUTX('CP_orig_classpath', STRIP(orig_classpath), 'GLOBAL');
	    CALL SYMPUTX('CP_path_separator', STRIP(path_separator), 'GLOBAL');
	    f.delete();
	RUN;
%mend;

/* 
Add a directory or JAR file to the Java classpath.
*/
%macro javautils_AddToClasspath(cp_addition);
	DATA _null_;
	    LENGTH  current_classpath $ 500
	            new_classpath $ 500;

	    current_classpath = STRIP(SYSGET("CLASSPATH"));

	    IF _ERROR_ = 1 OR LENGTH(current_classpath) = 0 THEN DO;
			PUT "NOTE: Ignore any messages from the next statement(s)";
	        new_classpath = "&cp_addition";
		END;
	    ELSE DO;
			IF FIND(current_classpath, "&cp_addition", "ti") = 0 THEN 
	        	new_classpath = STRIP(current_classpath) || "&CP_path_separator" || "&cp_addition";
			ELSE
				new_classpath = current_classpath;
		END;

	    CALL SYMPUTX('CP_new_classpath', STRIP(new_classpath), 'GLOBAL');
	RUN;

	%PUT NOTE: Setting Java classpath to &CP_new_classpath;
	OPTIONS SET=CLASSPATH "&CP_new_classpath";
%mend;

/*
Restore the classpath to its original state.
*/
%macro javautils_ResetClasspath;
	%PUT NOTE: Setting Java classpath back to its original state: &CP_orig_classpath;
	OPTIONS SET=CLASSPATH "&CP_orig_classpath";
%mend;


%macro toolbox_Start(classpath);
	%javautils_InitClasspathUpdate;
	%javautils_AddToClasspath(&classpath);
%mend;

%macro toolbox_End(classpath);
%mend;
