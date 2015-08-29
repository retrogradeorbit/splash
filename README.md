# splash

My small demo-scene-esque splash page. Test out using infinitelives.pixi with figwheel.

Checkout how it looks here:

    [www.procrustes.net](http://www.procrustes.net)

## Overview

My websites splash page. Idea is to make it a homage to 8-bit demoscene.

## Setup

Install infinitelives.utils and infinitelives.pixi.

Build assets

    make sfx

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2015 Crispin Wellington

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
