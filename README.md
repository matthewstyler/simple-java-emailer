# simple-java-emailer
A simple command line utility that can send email (params specified in a file) with an optional attachment

Requires java mail

<b>Usage: java SendEmail email_file attachment_file</b>

<i>Expected email file format

 Server: email server<br/>
 User: email account (also used as the From: in the email)<br/>
 Password: email account password <br/>
 To: primary recipient <br/>
 CC: comma separated list of secondary recipients <br/>
 BCC: comma separated list of tertiary recipients <br/>
 Subject: Email subject <br/>
 Body: multiple lines of text representing the body of the email</i>
