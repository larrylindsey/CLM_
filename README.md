CLM_
====

A collection of plugins written for tasks that may or may not be specific to the Kristen Harris Lab in the Center for Learning and Memory at UT Austin.

Plugins include:

TrakEM2_Archipelago
-------------------

Handles TrakEM2/Archipelago interoperability, notably including bottlers for points, layers, areas, sift parameters, patches and tiles, to allow for distributed alignment and montage operations.

Batch_Weka_Segmentation
-----------------------

Run Ignacio Arganda's [Trainable Weka Segmentation plugin](https://github.com/fiji/Trainable_Segmentation) in batch. This plugin is designed for 2D segmentation only, and currently depends on a running Fiji Archipelago Cluster (workaround: create a cluster and attach from the same machine).

AreaList_Crop
-------------

In TrakEM2, crop an EM stack by an area list.

Fix_Montage
-----------

This plugin was written to fix geometrical error introduced by montaging images in an external, non-scientific image processing platform (read: Photoshop). It is designed to replace the effect of the external montage with an elastic montage. Annotations are accounted for, but the original images must be available.

FS_Align_TrakEM2
----------------

Watches a folder for incoming images, and automagically aligns each new image to the previous one by least squares.

Reconstruct_Reader
------------------

Also a writer. [Reconstruct](http://synapses.clm.utexas.edu/tools/reconstruct/reconstruct.stm) project Importer/Exporter for TrakEM2.
