/* REXX                                                           */
/* PROGRAM: CALL A SHELL SCRIPT                                   */
/* INSTRUCTIONS:                                                  */
/* 1.Pass the path to the shell script that invokes the groovy    */
/* 2.input variables are 4 words separated by spaces and they are-*/
/*     username,                                                  */
/*     password,                                                  */
/*     ucd_server_url,                                            */
/*     process_request_id                                         */
cmd = 'sh /u/username/scripts/getStatusOfGenericProcess.sh'
INPUT="username password https://ucd_url:8443 request_id"
command_input = cmd||" "||input
code=bpxwunix(command_input,,out.,,,1)
      do i=1 to out.0
         say out.i
      end
