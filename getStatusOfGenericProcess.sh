#!bin/sh
# PROGRAM: CALL A GROOVY SCRIPT TO GET RESULT OF A UCD PROCESS
# INSTRUCTIONS:
#   1.  <AGENT-HOME> in AGENT_HOME_LIB should be replaced with a valid path to UCD-agent-home
#   2. <GROOVY-SCRIPT> should be replaced with absolute path getStatusOfGenericProcess.groovy

export _BPX_SPAWN_SCRIPT=YES
export _BPX_SHAREAS=NO
AGENT_HOME_LIB="<AGENT-HOME>/731/lib/zos/"
groovy -cp "$AGENT_HOME_LIB/uDeployRestClient.jar" <GROOVY-SCRIPT> "$1" "$2" "$3" "$4"