package fr.univavignon.transpolosearch;

/*
 * TranspoloSearch
 * Copyright 2015-17 Vincent Labatut
 * 
 * This file is part of TranspoloSearch.
 * 
 * TranspoloSearch is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 2 of the License, or (at your option) any later version.
 * 
 * TranspoloSearch is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.google.api.services.customsearch.model.Result;

import fr.univavignon.transpolosearch.extraction.Extractor;
import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univavignon.transpolosearch.search.web.GoogleEngine;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univavignon.transpolosearch.tools.string.StringTools;

/**
 * This class is used to launch some processes
 * testing the various features of the software.
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings({ "unused" })
public class Test
{	/**
	 * Basic main function, launches the
	 * required test. Designed to be modified
	 * and launched from Eclipse, no command-line
	 * options.
	 * 
	 * @param args
	 * 		None needed.
	 * 
	 * @throws Exception
	 * 		Something went wrong... 
	 */
	public static void main(String[] args) throws Exception
	{	
		// retrieval
//		testRetrievalGeneric();
		
		// search
//		testGoogleSearch();
		
		// whole process
		testExtractor();
		
		logger.close();
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	private static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// RETRIEVAL	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests the retrieval of text comming from random Web sites.
	 * 
	 * @throws Exception
	 * 		Problem during the retrieval.
	 */
	private static void testRetrievalGeneric() throws Exception
	{	logger.setName("Test-GenericRetrieval");
		logger.log("Start testing the generic Web page retrieval");
		logger.increaseOffset();
		
		// problem cases:
		// - comments longer than the article text
		// - all the article text in a single element (eg. <p>)
		// - page listing articles (instead of focusing on a single one)
		
//		URL url = new URL("http://www.lemonde.fr/culture/article/2014/07/16/la-prise-de-position-d-olivier-py-sur-le-fn-a-heurte-les-avignonnais_4457735_3246.html");
//		URL url = new URL("http://www.lemonde.fr/afrique/article/2015/05/02/au-togo-l-opposition-coincee-apres-son-echec-a-la-presidentielle_4626476_3212.html");
//		URL url = new URL("http://www.lemonde.fr/climat/article/2015/05/04/climat-les-energies-propres-en-panne-de-credits-de-recherche_4626656_1652612.html");
//		URL url = new URL("http://www.lemonde.fr/les-decodeurs/article/2015/05/03/les-cinq-infos-a-retenir-du-week-end_4626595_4355770.html");
//		URL url = new URL("http://www.lemonde.fr/les-decodeurs/article/2015/04/29/seisme-au-nepal-une-perte-economique-superieure-au-pib_4624928_4355770.html");
		
//		URL url = new URL("http://www.liberation.fr/vous/2015/05/04/coeur-carmat-le-deuxieme-greffe-decede-a-son-tour_1289323");
//		URL url = new URL("http://www.liberation.fr/societe/2015/05/04/femmes-en-politique-un-match-contre-les-machos_1289649");
//		URL url = new URL("http://www.liberation.fr/societe/2015/05/03/surveillance-le-flou-du-spectacle_1287003");
			
//		URL url = new URL("http://www.citylocalnews.com/avignon/2015/01/08/grand-avignon-cecile-helle-poursuit-son-lobby-ant-tram");
//		URL url = new URL("http://www.mobilicites.com/011-3377-Avignon-construira-bien-une-ligne-de-tramway.html");
//		URL url = new URL("http://www.citylocalnews.com/avignon/2015/01/26/avignon-des-policiers-a-cheval-bientot-sur-la-barthelasse");
//		URL url = new URL("https://fr-fr.facebook.com/pages/Les-Refondateurs/243905395803677");
//		URL url = new URL("http://www.citylocalnews.com/avignon/2015/01/10/avignon-le-tramway-divisera-t-il-la-majorite-municipale");
//		URL url = new URL("http://destimed.fr/Grand-Orient-de-France-conference");
//		URL url = new URL("http://www.medias-presse.info/le-grand-orient-nous-refait-le-coup-de-la-republique-en-danger/21996"); //xxxxxxxx plus de commentaires que d'article
//		URL url = new URL("http://www.meridienmag.fr/Actualites/ca-roule-pour-le-tramway-du-Grand-Avignon_2010.html");
//		URL url = new URL("http://www.grandavignon.fr/actualites/fiche/avignon-plan-proprete-tous-mobilises-tous-concernes/");
//		URL url = new URL("http://france3-regions.francetvinfo.fr/provence-alpes/2015/01/15/le-grand-avignon-aura-son-tramway-decouvrez-son-trace-632322.html");
//		URL url = new URL("http://www.midilibre.fr/2015/01/11/l-agglo-du-grand-avignon-vote-pour-le-tramway,1109359.php");
//		URL url = new URL("http://www.ville-rail-transports.com/content/21024-Avignon-Les-%C3%A9lus-votent-un-nouveau-projet-de-tram");
//		URL url = new URL("http://www.midilibre.fr/vaucluse/avignon/index_15.php");
//		URL url = new URL("http://www.liberation.fr/politiques/2015/01/08/regionales-vauzelle-ne-se-representera-pas-en-paca_1176199");
//		URL url = new URL("http://www.francebleu.fr/infos/chantier-tramway/le-tramway-en-questions-sur-france-bleu-vaucluse-2061645");
//		URL url = new URL("http://www.busetcar.com/actualites/detail/81676/le-tramway-d-avignon-verra-le-jour.html");
//		URL url = new URL("http://www.mavieenprovence.com/fr/tendances.html");
//		URL url = new URL("http://www.ledauphine.com/vaucluse/2015/01/06/avignon-tram-ou-pas-tram");
//		URL url = new URL("http://avignon.lafrancealunisson.fr/");
//		URL url = new URL("http://www.laprovence.com/actu/politique-en-direct/3203098/tramway-un-vote-convoque-samedi-pour-trancher.html");
//		URL url = new URL("http://www.ventoux-magazine.com/actualites/actualite/aeroport-d-avignon-le-pole-pegase-a-bien-decolle/x2132axyw2vMT79AhRm.html");
//		URL url = new URL("http://www.fm-mag.fr/evenement/antimaconnisme-refus-de-lautre-la-republique-en-danger"); //xxxxxxxx article trop court par rapport à la page
//		URL url = new URL("http://coordination-antinucleaire-sudest.net/2012/index.php?/page/2");
//		URL url = new URL("http://www.atlantico.fr/pepites/ps-michel-vauzelle-ne-se-representera-pas-presidence-region-paca-1945014.html");
//		URL url = new URL("http://www.leravi.org/spip.php?article1935");
//		URL url = new URL("http://www.infoavignon.com/actualites/politique-actualites/grand-avignon-cest-parti-pour-le-tram/");
//		URL url = new URL("http://raphael-helle.blog.lemonde.fr/2015/01/07/je-suis-charlie/");
//		URL url = new URL("http://www.clodelle45autrement.fr/2015/01/images-d-orleans-roller-derby-contre-hell-r-cheeky-dolls-la-rochelle-11-janvier-2015.html");
//		URL url = new URL("http://raphael-helle.blog.lemonde.fr/category/vie-privee/");
//		URL url = new URL("http://www.marcvuillemot.com/tag/var%20et%20intercommunalite/"); //xxxxxxxx liste d'articles

		URL url = new URL("http://www.univ-orleans.fr/espe/colloques");
//		URL url = new URL("http://www.lalogitude.com/albums/nos_adherents/index.html");
//		URL url = new URL("http://leslecturesdececile.fr/categorie/romance-historique/");
//		URL url = new URL("http://tvmag.lefigaro.fr/programme-tv/fiche/rmc-decouverte/serie-documentaire/190829701/highway-thru-hell-usa.html");
//		URL url = new URL("http://www.canalfrance.info/Marche-republicaine%E2%80%89-qui-seront-les-personnalites-presentes_a3173.html");
//		URL url = new URL("http://www.hellfest.fr/fr/pass-1-jour-a1084/");
//		URL url = new URL("https://pastel.archives-ouvertes.fr/tel-01109446/document");
//		URL url = new URL("http://www.gites-de-france-cotedor.com/location-Gite-Meursault-Cote-D-or-21G726.html");
//		URL url = new URL("http://tourner1page.fr/tag/mer/");
//		URL url = new URL("http://www.michelbachlebas.fr/mh-infos-municipales/presse");
//		URL url = new URL("http://prof.planck.fr/index.php?n=People.HFI-DPC");
//		URL url = new URL("http://www.enews-france.com/les-inrocks-secret-des-affaires-informer-n-est-pas-un-delit-128409-p");
//		URL url = new URL("http://www.lefigaro.fr/politique/2015/01/10/01002-20150110ARTFIG00160-l-impressionnante-liste-des-politiques-presents-a-la-marche-republicaine-a-paris.php");
//		URL url = new URL("http://blogs.mediapart.fr/edition/les-invites-de-mediapart/article/280115/secret-des-affaires-informer-n-est-pas-un-delit");
//		URL url = new URL("http://www.zicazic.com/zicazine/index.php?option=content&task=view&id=12130");
//		URL url = new URL("http://www.lesinrocks.com/2015/01/28/actualite/secret-des-affaires-informer-nest-pas-un-delit-11550918/");
//		URL url = new URL("http://univ-poitiers.academia.edu/CCM/Books");
//		URL url = new URL("http://www.tmplab.org/page/2/");
//		URL url = new URL("http://univ-poitiers.academia.edu/CCM");
//		URL url = new URL("http://www.commequiers.com/les-associatons-le-guide.html");
//		URL url = new URL("http://video.lefigaro.fr/figaro/video/la-chute-de-la-premiere-ministre-danoise-sur-les-marches-de-l-elysee/3984216817001/");
//		URL url = new URL("http://www.ville-de-wallers-arenberg.fr/wp/index.php/extensions/s5-tab-show-2/doc_download/88-bi-23");
//		URL url = new URL("http://www.400coups.net/archives/__crochet__/");
//		URL url = new URL("http://rakotoarison.over-blog.com/article-sr-125380563.html");
//		URL url = new URL("http://www.jesuismort.com/biographie_celebrite_anniversaire/5-eme_anniversaire_de_la_mort_de_celebrite.php");
//		URL url = new URL("http://cercletibetverite.unblog.fr/2015/01/");
//		URL url = new URL("http://www.shutupandplaythebooks.com/tu-mourras-moins-bete-marion-montaigne/");
//		URL url = new URL("http://bijoucontemporain.unblog.fr/category/ecole/gerrit-rietveld-academie-nl/");
//		URL url = new URL("https://les5duvin.wordpress.com/tag/salon-des-vins-de-loire/");
//		URL url = new URL("http://seminesaa.hypotheses.org/2842");
//		URL url = new URL("http://www.africine.org/index.php?menu=menuflm&smenu=format&choix_format=8&choix_trie=1");
//		URL url = new URL("http://www.agoravox.fr/tribune-libre/article/le-charlisme-est-un-humanisme-2-162338");
//		URL url = new URL("https://hal-mines-paristech.archives-ouvertes.fr/dumas-01102770/document");
//		URL url = new URL("http://www.babelio.com/livres/Green-Les-aventures-de-HawkFisher-tome-1/29897");
//		URL url = new URL("http://www.influencelesite.com/page/79");
//		URL url = new URL("http://www.unesourisdansmondressing.com/2015/01/22/avancer-malgre-la-tuile/");
//		URL url = new URL("http://www.ladepeche.fr/article/2015/01/06/");
//		URL url = new URL("http://www.cine-loisirs.fr/series/the-originals-2693/saison/2/episode/10");
//		URL url = new URL("http://pppl.blog.lemonde.fr/tag/robert-johnson/");
//		URL url = new URL("http://www.ideozmag.fr/je-suis-charlie-charlisme-est-un-humanisme-union-nationale/");
//		URL url = new URL("http://www.musicme.com/Vybz-Kartel/");
//		URL url = new URL("http://www.influencelesite.com/page/79");
//		URL url = new URL("http://www.unesourisdansmondressing.com/2015/01/22/avancer-malgre-la-tuile/");
//		URL url = new URL("http://www.ladepeche.fr/article/2015/01/06/");
//		URL url = new URL("http://www.cine-loisirs.fr/series/the-originals-2693/saison/2/episode/10");
//		URL url = new URL("http://pppl.blog.lemonde.fr/tag/robert-johnson/");
//		URL url = new URL("http://www.ideozmag.fr/je-suis-charlie-charlisme-est-un-humanisme-union-nationale/");
//		URL url = new URL("http://www.musicme.com/Vybz-Kartel/");
		
		ArticleRetriever retriever = new ArticleRetriever(false);
		retriever.process(url);
		
		logger.decreaseOffset();
		logger.log("Test terminated");
	}
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests specifically Google's custom search wrapper.
	 * 
	 * @throws Exception
	 * 		Something went wrong during the search. 
	 */
	private static void testGoogleSearch() throws Exception
	{	logger.setName("Test-GoogleEngine");
		logger.log("Start testing Google Custom Search");
		logger.increaseOffset();
		
		GoogleEngine gs = new GoogleEngine();
	
		// parameters
		String keywords = "Cécile Helle";
		String website = null;
		String sortCriterion = "date:r:20150101:20150131";
		
		// launch search
		List<Result> result = gs.searchGoogle(keywords, website, sortCriterion);
		
		List<String> msgs = new ArrayList<String>();
		logger.log("Displaying results: "+result.size()+"/"+gs.MAX_RES_NBR);
		logger.increaseOffset();
			int i = 0;
			for(Result res: result)
			{	i++;
				logger.log(Arrays.asList(
					"Result "+i+"/"+result.size(),
					res.getHtmlTitle(),
					res.getFormattedUrl(),
					"----------------------------------------")
				);
				msgs.add(res.getLink());
			}
		logger.decreaseOffset();
		
		logger.log(msgs);
		
		logger.decreaseOffset();
		logger.log("Test terminated");
	}

	/////////////////////////////////////////////////////////////////
	// WHOLE PROCESS	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests the whole information extraction process.
	 * 
	 * @throws Exception
	 * 		Something went wrong during the search. 
	 */
	private static void testExtractor() throws Exception
	{	Extractor extractor = new Extractor();
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		
//		String keywords = "Cécile Helle";
//		String compulsoryExpression = "Helle";
//		String keywords = "Martine Aubry";
//		String compulsoryExpression = "Aubry";
		String keywords = "Anne Hidalgo";
		String compulsoryExpression = "Hidalgo";
		
		String website = null;
		Date startDate = df.parse("20170306");
		Date endDate = df.parse("20170310");
		boolean searchDate = true;
		boolean extendedSocialSearch = true;
		
		extractor.performExtraction(keywords, website, startDate, endDate, searchDate, compulsoryExpression, extendedSocialSearch);
	}
}

// dates: 6/3 >> 10/3
// pers: C. Helle + M. Aubry + A. Hidalgo

/** TODO
 * - edit the SNS search part, mutualize the new classes designed from the web search
 * - check the retrieved pages, possibly correct the software when needed
 * 
 * - process each engine separately, record the results in their individual folders, then only merge the results
 * - check if articles have unique name/title (they should)
 */
