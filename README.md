# anole

A Clojure library designed to alter the base colors of a bitmap
but retain the anti-aliasing scheme of the original colors. This
library is designed with website icons in mind, where there are
at most a few main colors, and the file-sizes are relatively small.
Performace on large files with many colors may yield unexpected results.

## Usage

An image may be read using the (read-image [file-name]) function, which
returns a java.awt.image.BufferedImage.

A new BufferedImage with its colors altered is returned from the
(alter-image [buffered-image main-colors-swap-map]) function. The second
argument to this function expects a clojure map in the form of:
{<-first-original-color> <-first-new-color>, <-second-original-color> <-second-new-color>...}
The map can contain an arbitrary number of color-pairs. If there is a color
that is NOT intended to be altered, it must be listed in the map twice,
in both the new and original positions. This function expects the mapped
colors to be in the form of integers. If the java.awt.Color class is being
used, integers can be obtained via calling its .getRGB method.


## License

Copyright © 2015 John Curtis Bailey

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
