# Introduction #

Generally speaking, this app is set up to consume data similar to the way the web mapping apps work - with a set of base tiles, overlaid with a minimal amount of vector data. bcMapper relies on the nutiteq library as the core mapping library.

## Base Tiles ##
There are several on-line map services bcMapper is pre-configured to use, including Bing street and aerial maps, OSM maps, and Cloudmade. It will also support a local tile cache from ArcGIS server, the GDAL2Tiles.py tool, OSM cache, etc saved on sdcard or internal memory.

## Overlay data ##
Right now KML is the only format supported for overlay data. This can be either an online URL or a local file saved on sdcard or disk. Not all KML-supported data types, like images, are supported (currently). Points, lines, and polygons are supported. KMZ files will also be supported.