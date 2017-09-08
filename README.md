TranspoloSearch
=======
*Web-based information extraction for political science*

* Copyright 2015-17 Vincent Labatut

TranspoloSearch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see `licence.txt`

* Lab site: http://lia.univ-avignon.fr/
* GitHub repo: https://github.com/CompNet/TranspoloSearch
* Contact: vincent.labatut@univ-avignon.fr

-----------------------------------------------------------------------

## Description
This software is currently in development. Don't use it (yet)!

## Organization

## Installation

## Use

## Extension

## Dependencies
Here are the dependencies for TranspoloSearch:
* A bunch of JARs from the [Google APIs Client Library for Java](https://developers.google.com/api-client-library/java/apis/customsearch/v1)
* [jsoup](http://jsoup.org/) to handle HTML files 
* [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/)
* [JSON.simple](https://code.google.com/archive/p/json-simple/) to parse JSON documents
* Certain classes (mainly those related to Named Entity Recognition) were taken from our own tool [Nerwip](https://github.com/CompNet/Nerwip) (and sometimes modified)

## Todo
* Define a black list corresponding to satirical journals (Gorafi, Infos du monde, Nordpresse, Sud ou Est, etc.)
* Article filtering: once the content has been retrieved, filter articles not published during the targeted period (they could describe events taking place during this period though, so maybe only article published before the period?)
* Article retrieval:
  * Fine-tune the generic reader by considering all the articles in a given corpus which are too short (less than 1000 characters) or too long (more than 3000 characters?). One identified problem is the case of pages containig not one article, but rather a list of articles (sometimes with the first paragraph of each article).
  * Maybe use the Boilerpipe API instead of our custom tool? (see https://code.google.com/archive/p/boilerpipe and https://github.com/kohlschutter/boilerpipe)
  * For some sources, only a part of certain articles is available (restricted access, requiring some sort of registration). We could set up a reader-specific option, in order to allow to either: 1) give up the retrieval in this case (interesting if the restriction is only temporary, eg. you can access the article next month); or 2) get what we can, i.e. generally the first paragraphs (this is the current behavior, which is appropriate if the rest of the article will never be available). 
  * Add the specifically defined readers for the following information sites:
    * 20 Minutes (www.20minutes.fr)
    * Arrêt sur Images (www.arretsurimages.net)
    * Atlantico (www.atlantico.fr)
    * Au Féminin (www.aufeminin.com)
    * BFM TV (www.bfmtv.com)
    * Capital (www.capital.fr)
    * Closer (www.closermag.fr)
    * Dernières Nouvelles d'Alsace (www.dna.fr)
    * Europe 1 (www.europe1.fr)
    * France Culture (www.franceculture.fr)
    * France TV (www.france.tv)
    * France TV Info (www.francetvinfo.fr)
    * Huffington Post France (www.huffingtonpost.fr)
    * JeuxVidéos.com (www.jeuxvideo.com)
    * L'Equipe (www.lequipe.fr)
    * L'Est Républicain (www.estrepublicain.fr)
    * L'Opinion (www.lopinion.fr)
    * La Croix (www.la-croix.com)
    * La Croix du Nord (www.croixdunord)
    * La Dépêche du Midi (www.ladepeche.fr)
    * La Tribune (www.latribune.fr)
    * LCP (www.lcp.fr)
    * Le Dauphiné Libéré (www.ledauphine.com)
    * Le Petit Journal (www.lepetitjournal.com)
    * Les Echos (www.lesechos.fr)
    * Marianne (www.marianne.net)
    * Mediapart (www.mediapart.fr)
    * Nord-Eclair (www.nordeclair.fr)
    * Ouest France (www.ouest-france.fr)
    * Paris Match (www.parismatch.com)
    * Paris Normandie (www.paris-normandie.fr)
    * RFI (www.rfi.fr)
    * RTBF (www.rtbf.be)
    * RTL (www.rtl.fr)
    * Rue 89 (tempsreel.nouvelobs.com/rue89/) (and regional variants, e.g. www.rue89strasbourg.com)
    * Sciences et Avenir (www.sciencesetavenir.fr)
    * Sud Ouest (www.sudouest.fr)
    * TF1 (www.tf1.fr)
    * Valeurs Actuelles (www.valeursactuelles.com)
    * Voici (www.voici.fr)
* Search engines:
  * Add the Duck Duck Go search engine. As of 2017/04/21, the Instant Answer API is too restricted to return results we could use in TranspoloSearch. See [this page](https://api.duckduckgo.com/api) for a description of the API. Also, it is powered by other search engines already integrated in TranspoloSearch.
  * Add the Yahoo search engine. Apparently, Yahoo is powered by Bing since 2011, so not worth it since we already have Bing.
  * Add the Baidu search engine. As of 2017/04/22, the documentation is in Chinese only (https://www.programmableweb.com/api/baidu).
  * Add the Orange search engine, which focuses on French (http://www.lemoteur.fr/).
  * Add the BoardReader search engine, which focuses on Q/A and Forum websites (http://boardreader.com/).
  * Add the Exalead search engine, originally designed for intranets (https://www.exalead.com/search/web/).  
  * Add Twitter support.

## References
[MLE'15] [Le Web comme miroir du travail politique quotidien ? Reconstituer l'écho médiatique en ligne des événements d'un agenda d'élu](http://agorantic.univ-avignon.fr/wp-content/uploads/sites/13/2014/10/Publications-Agorantic1.pdf). G. Marrel, V. Labatut & M. El Bèze. 13ème Congrès de l'Association Française de Science Politique (AFSP), 2015, 25p.
