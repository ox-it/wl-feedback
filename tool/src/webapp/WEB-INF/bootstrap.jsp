<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  
      xmlns="http://www.w3.org/1999/xhtml"
      xml:lang="${language}"
      lang="${language}">
<head>
<link rel="stylesheet" type="text/css" href="/feedback-tool/css/feedback.css" />
<script type="text/javascript" src="/feedback-tool/lib/jquery-1.9.1.min.js"></script>
<script type="text/javascript" src="/feedback-tool/lib/handlebars.runtime-v1.3.0.js"></script>
<script type="text/javascript" src="/feedback-tool/js/sakaii18nloader.js"></script>

<!-- HANDLEBARS TEMPLATES START -->
<!--
<script type="text/javascript" src="/feedback-tool/templates/home.handlebars"></script>
<script type="text/javascript" src="/feedback-tool/templates/content.handlebars"></script>
<script type="text/javascript" src="/feedback-tool/templates/functionality.handlebars"></script>
-->
<script type="text/javascript" src="/feedback-tool/templates/all.handlebars"></script>
<!-- HANDLEBARS TEMPLATES END -->

<script type="text/javascript">

    var feedback = {
        startupArgs: {
            state: 'home',
            userId: '${userId}',
            language: '${language}'
        }
    };
    
</script>
<script type="text/javascript" src="/feedback-tool/js/feedbackutils.js"></script>
${sakaiHtmlHead}
</head>
<body>

<!-- wrap tool in portletBody div for PDA portal compatibility -->
<div id="feedback-content" class="portletBody">
</div>
<script type="text/javascript" src="/feedback-tool/js/feedback.js"></script>

</body>
</html>
