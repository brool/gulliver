Gulliver
========

A lightweight web development framework, building on top of
Clojure/Compojure.  (Highly experimental and draft).  Currently allows
template files with embedded Clojure.

Using Clojure Templates
-----------------------

There is a default template server defined that tries to load the file
against the current directory, and returns the executed template.

To set up a server, all that is required is::

    (use 'gulliver)

    (defserver web-server
      {:port 8080}
      "/*"  (servlet gulliver/template-servlet))

    (start web-server)

Details
-------

Each file is compiled into a single routine, render, which takes the
request.  Every file has its own namespace; for example, if you have a
file /admin/edituser.clj, then the render routine would be
admin.edituser.clj/render.

Example
-------

An example::

    <?
    (import 'java.util.Date)
    ?>

    <html>
      <body>
        <h1>Delivered via Gulliver</h1>
        <p>Expressions at work: <?= (+ 2 2) ?></p>
        <p>Looping:</p>
        <ul>
        <? (dotimes [x 10] ?>
          <li><?= x ?></li>
        <? ) ?>
        </ul>

        <p>Here's a date:  <?= (Date.) ?></p>
      </body>
    </html>

Todo
----

- Intrafile references.  need a way of referring to the code in other
  files.  You can use the "use" macro, but if the other file hasn't
  been loaded yet then the namespace won't exist.  Ugh.  Probably need
  to add a "require" equivalent a la PHP.  

- Performance.  Currently, a stat is done everytime a page is
  rendered; it should really on a variable expire.

- Need to make it easier to access params and whatnot.
