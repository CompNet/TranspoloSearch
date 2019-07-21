TranspoloSearch v2
=======
*Web-based information extraction for political science*

* Copyright 2015-18 Vincent Labatut

TranspoloSearch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see `licence.txt`

* Lab site: http://lia.univ-avignon.fr/
* GitHub repo: https://github.com/CompNet/TranspoloSearch
* Contact: vincent.labatut@univ-avignon.fr

-----------------------------------------------------------------------

## Description
This software takes the name of a public person and a period, and retrieve all events available online involving this person during this period. It first perform a web search using various engines, then retrieves the corresponding Web pages, performs NER (named entity recognition), uses these entities to cluster the articles, and considers each cluster as the description of a specific event. It is designed to handle Web pages in French, but should work also for English. It has been used in references [MLE'15] and [ML'17].


## Organization
The source code takes the form of an Eclipse project. It is organized as follows: 
* Package `data` contains all the classes used to represent data: articles, entities, etc.
* Pacakge `evaluation` contains classes used to measure the performance of the retrieval tool
* Package `processing` contains classes related to named entity recognition (NER).
* Package `retrieval` contains classes used to get the web pages.
* Package `search` contains classes used to perform the web search.
* Package `tools`: various classes used throughout the software.

The rest of the files are resources:
* Folder `lib` contains the external libraries, especially the NER-related ones (cf. the *Dependencies* section).
* Folder `log` contains the log generated during the processing.
* Folder `out` contains the articles and the files generated during the process. 
* Folder `res` contains the XML schemas (XSD files), as well as the configuration files required by certain NER tools.


## Installation
First, get the last version of the project. Second, you need to download some additional files to get the required data.

Most of the data files are too large to be compatible with GitHub constraints. For this reason, they are hosted on [FigShare](https://doi.org/10.6084/m9.figshare.1289791). Before using Nerwip, you need to retrieve these archives and unzip them in the Eclipse project.

1. Go to our FigShare page https://doi.org/10.6084/m9.figshare.1289791
2. You need the data related to the different NER tools (models, dictionaries, etc.), and you can ignore the corpus files (used for another project).
  * Download all 4 Zip files containing the NER data,
  * Extract the `res` folder,  
  * Put it in the Eclipse project, in place of the existing `res` folder. **Do not** remove the existing folder, just overwrite it (we need the existing folders and files).

Finally, some of the NER tools integrated in Nerwip require some key or password to work. This is the case of:
* Subee: our Wikipedia/Freebase-based NER tool requires a Freebase key to work correctly.
* OpenCalais: this NER tool takes the form of a Web service.
All keys are set up in the dedicated XML file `keys.xml`, which is located in `res/misc`.


## Use
For now, there is not interface, not even a command-line one. All the processes need to be launched programmatically, as illustrated by class `fr.univavignon.transpolosearch.Test`. I advise to import the project in Eclipse and directly edit the source code in this class. A more appropriate interface will be added once the software is more stable. The output folder is `out`.


## Dependencies
Here are the dependencies for TranspoloSearch:
* Misc.:
  * A bunch of JARs from the [Google APIs Client Library for Java](https://developers.google.com/api-client-library/java/apis/customsearch/v1)
  * [jsoup](http://jsoup.org/) to handle HTML files 
  * [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/)
  * [JSON.simple](https://code.google.com/archive/p/json-simple/) to parse JSON documents
  * [JSTAT](https://github.com/EdwardRaff/JSAT) to cluster events
* NER Tools:
  * Libraries:
    * [alias-i LingPipe](http://alias-i.com/lingpipe/)
    * [HeidelTime](https://code.google.com/p/heideltime/)
    * [Nero](https://nero.irisa.fr/)
    * [TagEN](http://gurau-audibert.hd.free.fr/josdblog/wp-content/uploads/2008/03/TagEN.tar.gz)
    * Certain classes were taken from our own tool [Nerwip](https://github.com/CompNet/Nerwip) (and sometimes modified)
  * Web services:
    * [Thomson Reuters OpenCalais](http://new.opencalais.com/)
    * [OpeNER](http://www.opener-project.eu/)
  * Libraries required by certain NER tools:
    * [TreeTagger](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/), needed by HeidelTime.
  * Non-included libraries: some libraries are not included and must be installed manually.
    * [OpenFST](http://www.openfst.org/), needed by Nero (see its `README` file in folder `res`, for instructions).
    * [Wapiti](http://wapiti.limsi.fr/), needed by Nero (again, see its `README` file in folder `res`, for instructions).


## Todo
* Define a black list corresponding to satirical journals (*Gorafi*, *Infos du monde*, *Nordpresse*, *Sud ou Est*, etc.)
* Article filtering: once the content has been retrieved, filter articles not published during the targeted period (they could describe events taking place during this period though, so maybe only article published before the period?)
* Article retrieval:
  * Fine-tune the generic reader by considering all the articles in a given corpus which are too short (less than 1000 characters) or too long (more than 3 000 characters?). One identified problem is the case of pages containing not one article, but rather a list of articles (sometimes with the first paragraph of each article).
  * Maybe use the Boilerpipe API instead of our custom tool? (see https://code.google.com/archive/p/boilerpipe and https://github.com/kohlschutter/boilerpipe)
  * For some sources, only a part of certain articles is available (restricted access, requiring some sort of registration). We could set up a reader-specific option, in order to allow to either: 1) give up the retrieval in this case (interesting if the restriction is only temporary, eg. you can access the article next month); or 2) get what we can, i.e. generally the first paragraphs (this is the current behavior, which is appropriate if the rest of the article will never be available). 
  * Add the specifically defined readers for the following information sites:
    * [20 Minutes](www.20minutes.fr)
    * [Arrêt sur Images](www.arretsurimages.net)
    * [Atlantico](www.atlantico.fr)
    * [Au Féminin](www.aufeminin.com)
    * [BFM TV](www.bfmtv.com)
    * [Capital](www.capital.fr)
    * [Closer](www.closermag.fr)
    * [Dernières Nouvelles d'Alsace](www.dna.fr)
    * [Europe 1](www.europe1.fr)
    * [France Culture](www.franceculture.fr)
    * [France TV](www.france.tv)
    * [France TV Info](www.francetvinfo.fr)
    * [Huffington Post France](www.huffingtonpost.fr)
    * [JeuxVidéos.com](www.jeuxvideo.com)
    * [L'Equipe](www.lequipe.fr)
    * [L'Est Républicain](www.estrepublicain.fr)
    * [L'Opinion](www.lopinion.fr)
    * [La Croix](www.la-croix.com)
    * [La Croix du Nord](www.croixdunord)
    * [La Dépêche du Midi](www.ladepeche.fr)
    * [La Tribune](www.latribune.fr)
    * [LCP](www.lcp.fr)
    * [Le Dauphiné Libéré](www.ledauphine.com)
    * [Le Petit Journal](www.lepetitjournal.com)
    * [Les Echos](www.lesechos.fr)
    * [Marianne](www.marianne.net)
    * [Mediapart](www.mediapart.fr)
    * [Nord-Eclair](www.nordeclair.fr)
    * [Ouest France](www.ouest-france.fr)
    * [Paris Match](www.parismatch.com)
    * [Paris Normandie](www.paris-normandie.fr)
    * [RFI](www.rfi.fr)
    * [RTBF](www.rtbf.be)
    * [RTL](www.rtl.fr)
    * [Rue 89](tempsreel.nouvelobs.com/rue89/) (and regional variants, e.g. www.rue89strasbourg.com)
    * [Sciences et Avenir](www.sciencesetavenir.fr)
    * [Sud Ouest](www.sudouest.fr)
    * [TF1](www.tf1.fr)
    * [Valeurs Actuelles](www.valeursactuelles.com)
    * [Voici](www.voici.fr)
  * For these journal-specific readers, we could define a generic process consisting in looking for an HTML element with a predefined class for authors, another for title, etc. One would just have to define the appropriate classes (or other HTML info): updating such reader would be easier.
* Search engines:
  * Add the Duck Duck Go search engine. As of 2017/04/21, the Instant Answer API is too restricted to return results we could use in TranspoloSearch. See [this page](https://api.duckduckgo.com/api) for a description of the API. Also, it is powered by other search engines already integrated in TranspoloSearch.
  * Add the Yahoo search engine. Apparently, Yahoo is powered by Bing since 2011, so not worth it since we already have Bing.
  * Add the Baidu search engine. As of 2017/04/22, the documentation is in Chinese only (https://www.programmableweb.com/api/baidu).
  * Add the Orange search engine, which focuses on French (http://www.lemoteur.fr/).
  * Add the BoardReader search engine, which focuses on Q/A and Forum websites (http://boardreader.com/).
  * Add the Exalead search engine, originally designed for intranets (https://www.exalead.com/search/web/).  
  * Add Twitter support.


## References
* **[ML'17]** V. Labatut & G. Marrel. *La visibilité politique en ligne : Contribution à la mesure de l’e-reputation politique d’un maire urbain*, Big Data et visibilité en ligne - Un enjeu pluridisciplinaire de l’économie numérique, 32p, 2017. [⟨hal-01904352⟩](https://hal.archives-ouvertes.fr/hal-01904352)
* **[MLE'15]** G. Marrel, V. Labatut & M. El Bèze. *Le Web comme miroir du travail politique quotidien ? : Reconstituer l'écho médiatique en ligne des événements d'un agenda d'élu*, 13ème Congrès de l'Association Française de Science Politique (AFSP), 25p, 2015. [⟨hal-01904338⟩](https://hal.archives-ouvertes.fr/hal-01904338)
