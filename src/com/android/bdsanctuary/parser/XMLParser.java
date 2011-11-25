package com.android.bdsanctuary.parser;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.Uri;

import com.android.bdsanctuary.R;
import com.android.bdsanctuary.datas.Global;
import com.android.bdsanctuary.datas.Serie;
import com.android.bdsanctuary.datas.Serie.Status;
import com.android.bdsanctuary.datas.Tome;
import com.cyrilpottiers.androlib.Log;

public class XMLParser {

    public static ArrayList<Serie> parseSeriesXML(String result)
            throws Exception {
        Elements nodeList = null;
        ArrayList<Serie> series = new ArrayList<Serie>();
        Serie serie;
        try {
            Document doc = Jsoup.parse(result);

            // table collection
            nodeList = doc.getElementsByClass("collection");

            Element table = nodeList.first();

            nodeList = table.getElementsByAttributeValueStarting("class", "color_status_");

            for (Element node : nodeList) {
                if ((serie = parseSerieXML(node)) != null) series.add(serie);
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
        }
        return series;
    }

    private static Serie parseSerieXML(Element node) {
        Serie serie = new Serie();
        String s;
        // status
        if (node.hasAttr("name")) {
            s = node.attr("name");
            if ("blue".equalsIgnoreCase(s))
                serie.setStatus(Status.SUIVIE);
            else if ("green".equalsIgnoreCase(s))
                serie.setStatus(Status.COMPLETE);
            else if ("red".equalsIgnoreCase(s))
                serie.setStatus(Status.NON_SUIVIE);
            else if ("orange".equalsIgnoreCase(s))
                serie.setStatus(Status.INTERROMPUE);
        }

        Elements nodeList = node.children();
        int i;
        for (Element tempNode : nodeList) {
            if ("td".equalsIgnoreCase(tempNode.nodeName())
                && tempNode.hasAttr("class")) {
                String c = tempNode.attr("class");
                if ("nom_serie".equalsIgnoreCase(c)) {
                    tempNode = tempNode.child(0);
                    Log.d(Global.getLogTag(XMLParser.class), "nom_serie="
                        + tempNode.ownText());
                    serie.setName(tempNode.ownText());
                }
                else if ("volume".equalsIgnoreCase(c)) {
                    Log.d(Global.getLogTag(XMLParser.class), "volume="
                        + tempNode.ownText());
                    i = Integer.parseInt(tempNode.ownText());
                    serie.setTomeCount(i);
                }
                else if ("support".equalsIgnoreCase(c)) {
                    Elements imgs = tempNode.getElementsByTag("img");
                    for (Element img : imgs) {
                        if (img.hasAttr("id")) {
                            Log.d(Global.getLogTag(XMLParser.class), "id="
                                + img.attr("id"));
                            i = Integer.parseInt(img.attr("id"));
                            serie.setId(i);
                            break;
                        }
                    }
                }
            }
        }
        String attr = null;
        Uri uri = null;
        while (true) {
            node = node.nextElementSibling();
            if (node == null || node.attr("name") == null
                || !node.attr("name").equals(Integer.toString(serie.getId())))
                break;
            nodeList = node.children();
            int id_edition = -1;
            int volume_count = -1;
            for (Element tempNode : nodeList) {
                if ("td".equalsIgnoreCase(tempNode.nodeName())
                    && tempNode.hasAttr("class")) {
                    String c = tempNode.attr("class");
                    if ("nom_serie".equalsIgnoreCase(c)) {
                        Elements anchors = tempNode.getElementsByTag("a");
                        for (Element anchor : anchors) {
                            if (anchor.hasAttr("href")) {
                                attr = anchor.attr("href");
                                try {
                                    uri = Uri.parse(new StringBuilder().append(Global.getResources().getString(R.string.MS_ROOT)).append(attr).toString());
                                    id_edition = Integer.parseInt(uri.getQueryParameter("id_edition"));
                                }
                                catch (Exception e) {
                                    Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                                }
                                break;
                            }
                        }
                    }
                    else if ("volume".equalsIgnoreCase(c)) {
                        try {
                            attr = tempNode.ownText();
                            attr = attr.substring(0, attr.indexOf('/'));
                            volume_count = Integer.parseInt(attr);
                        }
                        catch (Exception e) {
                            Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                        }
                    }
                }
            }
            if (id_edition >= 0 && volume_count >= 0) {
                serie.addEdition(id_edition, volume_count);
            }
        }
        //        Log.d(Global.TAG, "next element="+node.outerHtml());

        return serie;
    }

    public static ArrayList<Tome> parseEditionXML(String result)
            throws Exception {

        Log.w(Global.getLogTag(XMLParser.class), "parseEditionXML : \n"
            + result);

        Elements nodeList = null;
        ArrayList<Tome> tomes = new ArrayList<Tome>();
        Tome tome;
        Element child;
        String sId, sNumber, sUrl, sPageUrl;
        int iId, iNumber;
        try {
            Document doc = Jsoup.parse(result);

            nodeList = doc.getElementsByAttributeValueStarting("class", "titre_encart_");

            for (Element node : nodeList) {
                Log.d(Global.getLogTag(XMLParser.class), "span : \n"
                    + node.outerHtml());
                tome = new Tome();
                sId = sNumber = sUrl = sPageUrl = null;
                iId = 0;
                child = node.getElementsByTag("input").first();
                if (child.hasAttr("value")) sId = child.attr("value");
                child = node.getElementsByTag("a").first();
                if (child.hasAttr("href")) sPageUrl = child.attr("href");
                sNumber = child.ownText();
                sNumber = sNumber.substring(sNumber.indexOf('#') + 1).trim();

                node = node.nextElementSibling();
                Log.d(Global.getLogTag(XMLParser.class), "li : \n"
                    + node.outerHtml());
                child = node.getElementsByTag("a").first();
                child = child.getElementsByTag("img").first();
                if (child.hasAttr("src")) sUrl = child.attr("src");
                if (sUrl != null) {
                    if (sUrl.indexOf('/') != 0 && !sUrl.startsWith("http"))
                        sUrl = new StringBuilder().append('/').append(sUrl).toString();

                    int i = sUrl.lastIndexOf('/') + 1;
                    int j = sUrl.lastIndexOf('.');
                    if (j > 0)
                        sUrl = new StringBuilder(sUrl.substring(0, i)).append(URLEncoder.encode(sUrl.substring(i, j))).append(sUrl.substring(j)).toString();
                    else
                        sUrl = new StringBuilder(sUrl.substring(0, i)).append(URLEncoder.encode(sUrl.substring(i))).toString();

                }

                try {
                    iId = Integer.parseInt(sId);
                    tome.setId(iId);
                }
                catch (Exception e) {
                    Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                }
                try {
                    iNumber = Integer.parseInt(sNumber);
                    tome.setNumber(iNumber);
                }
                catch (Exception e) {
                    Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                }
                tome.setIconUrl(sUrl);
                tome.setTomePageUrl(sPageUrl);
                Log.i(Global.getLogTag(XMLParser.class), "  --> " + iId + ","
                    + sNumber + "," + sUrl);

                tomes.add(tome);
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
        }
        return tomes;
    }

    public static ArrayList<Tome> parseMissingTomesXML(String result) {
        Elements nodeList = null;
        Element node = null;
        ArrayList<Tome> tomes = new ArrayList<Tome>();
        Tome tome = null;
        Serie currentSerie = null;
        int i, num = 0;
        String url = null;
        String pattern = "T\\.(\\d+) -";
        Pattern p = Pattern.compile(pattern);
        try {
            Document doc = Jsoup.parse(result);

            // table collection
            node = doc.getElementsByClass("collection").first();
            nodeList = node.getElementsByTag("tr");

            for (i = 0; i < nodeList.size(); i++) {
                node = nodeList.get(i);
                // zap headers
                if (i == 0) continue;
                // new serie
                if (!node.hasAttr("class")) {
                    node = node.getElementsByClass("nom_serie_gros").first();
                    Log.v(Global.getLogTag(XMLParser.class), "missing xml serie node="
                        + node.outerHtml() + " text=" + node.ownText());
                    currentSerie = Global.getAdaptor().lookupSerieFromName(node.ownText());
                }
                else if (currentSerie != null) {
                    node = node.getElementsByTag("a").first();
                    Log.v(Global.getLogTag(XMLParser.class), "missing xml tome node="
                        + node.outerHtml() + " text=" + node.ownText());
                    tome = new Tome();
                    tome.setSerieId(currentSerie.getId());
                    try {
                        Matcher m = p.matcher(node.ownText());
                        if (m.find()) {
                            num = Integer.parseInt(m.group(1));
                        }
                        tome.setNumber(num);
                        url = node.attr("href");
                        tome.setTomePageUrl(url);
                        tome.isMissingTome = true;
                        tomes.add(tome);
                    }
                    catch (Exception e) {
                        Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                    }
                }
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
            return null;
        }
        return tomes;
    }

    public static ArrayList<Tome> parseMissingTomesMobileXML(String result) {
        Element node = null;
        ArrayList<Tome> tomes = new ArrayList<Tome>();
        Tome tome = null;
        Serie currentSerie = null;
        int num;
        String url = null;
        try {
            Document doc = Jsoup.parse(result);

            // table collection
            node = doc.getElementsByTag("h3").first();
            while (node != null) {
                if (node.tagName().equals("h3")) {
                    Log.v(Global.getLogTag(XMLParser.class), "missing xml serie node="
                        + node.ownText());
                    currentSerie = Global.getAdaptor().lookupSerieFromName(node.ownText());
                }
                else if (node.tagName().equals("a") && currentSerie != null) {
                    Log.v(Global.getLogTag(XMLParser.class), "missing xml tome node="
                        + node.ownText());
                    tome = new Tome();
                    tome.setSerieId(currentSerie.getId());
                    try {
                        num = Integer.parseInt(node.ownText().substring(node.ownText().lastIndexOf(' ') + 1));
                        url = node.attr("href");
                        tome.setNumber(num);
                        tome.setTomePageUrl(url);
                        tome.isMissingTome = true;
                        tomes.add(tome);
                    }
                    catch (Exception e) {
                        Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
                    }
                }
                else {
                    Log.v(Global.getLogTag(XMLParser.class), "missing xml zap node="
                        + node.outerHtml());
                }
                node = node.nextElementSibling();
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(XMLParser.class), e);
            return null;
        }
        return tomes;
    }

}
