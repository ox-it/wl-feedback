Sakai Feeback Tool
==================

Overview
--------

This tool allows a Sakai user to report problems with a site's content or
functionality. Reports are sent as emails to the problem site's contact email,
or, if that hasn't been specified, to a user selected site maintainer. In
addition to the two reports, there is a link to an area where you can suggest
new features. This link has to be configured from sakai.properties.

Developers
----------

The feedback tool is written using a mixture of Java, Javascript and Handlebars
templates. A servlet (FeedbackServlet.java) and JSP page (bootstrap.jsp) are
used to initialise the page shell with Javascript variables. Javascript then
takes over and renders templates into the shell when a link is clicked. Forms
are submitted to an EntityProvider (FeedbackEntityProvider.java) and that
provider does the emailing. Some protection against DDoS attacks is provided by
an optional Recaptcha integration.
