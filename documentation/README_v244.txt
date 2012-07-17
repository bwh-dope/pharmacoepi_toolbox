IMPORTANT NOTE
--------------

Version 2.4.4 of the PE Toolbox changes an important internal parameter in the hd-PS variable selection process.  In prior versions, a code had to occur at least 100 times across the population in order to be considered for inclusion.  This version removes that requirement, but to maintain compatibility with older versions, a minimum frequency can be imposed by setting the option:

	frequency_min = 100
	
Other values of minimum frequency can also be set as desired.  Setting the minimum to 0 indicates that no minimum threshold must be met.

