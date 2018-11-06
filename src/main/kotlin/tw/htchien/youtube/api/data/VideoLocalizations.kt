/*
 * Copyright (c) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tw.htchien.youtube.api.data

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.ArrayMap
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.VideoLocalization
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample sets and retrieves localized metadata for a video by:
 *
 * 1. Updating language of the default metadata and setting localized metadata
 * for a video via "videos.update" method.
 * 2. Getting the localized metadata for a video in a selected language using the
 * "videos.list" method and setting the "hl" parameter.
 * 3. Listing the localized metadata for a video using the "videos.list" method and
 * including "localizations" in the "part" parameter.
 *
 * @author Ibrahim Ulukaya
 */
object VideoLocalizations {

    /**
     * Define a global instance of a YouTube object, which will be used to make
     * YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Prompt the user to enter the language for the resource's default metadata.
     * Then return the language.
     */
    private// If nothing is entered, defaults to "en".
    val defaultLanguage: String
        @Throws(IOException::class)
        get() {

            var defaultlanguage = ""

            print("Please enter the language for the resource's default metadata: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            defaultlanguage = bReader.readLine()

            if (defaultlanguage.length < 1) {
                defaultlanguage = "en"
            }

            println("You chose " + defaultlanguage +
                    " as the language for the resource's default metadata.")
            return defaultlanguage
        }

    /*
     * Prompt the user to enter a language for the localized metadata. Then return the language.
     */
    private// If nothing is entered, defaults to "de".
    val language: String
        @Throws(IOException::class)
        get() {

            var language = ""

            print("Please enter the localized metadata language: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            language = bReader.readLine()

            if (language.length < 1) {
                language = "de"
            }

            println("You chose $language as the localized metadata language.")
            return language
        }

    /*
     * Prompt the user to enter an action. Then return the action.
     */
    private val actionFromUser: String
        @Throws(IOException::class)
        get() {

            var action = ""

            print("Please choose action to be accomplished: ")
            print("Options are: 'set', 'get' and 'list' ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            action = bReader.readLine()

            return action
        }


    /**
     * Set and retrieve localized metadata for a video.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "localizations")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-localizations-sample").build()

            // Prompt the user to specify the action of the be achieved.
            val actionString = actionFromUser
            println("You chose $actionString.")
            //Map the user input to the enum values.
            val action = Action.valueOf(actionString.toUpperCase())

            when (action) {
                Action.SET -> setVideoLocalization(getId("video"), defaultLanguage,
                        language, getMetadata("title"), getMetadata("description"))
                Action.GET -> getVideoLocalization(getId("video"), language)
                Action.LIST -> listVideoLocalizations(getId("video"))
            }
        } catch (e: GoogleJsonResponseException) {
            System.err.println("GoogleJsonResponseException code: " + e.details.code
                    + " : " + e.details.message)
            e.printStackTrace()

        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.message)
            t.printStackTrace()
        }

    }

    /**
     * Updates a video's default language and sets its localized metadata.
     *
     * @param videoId The id parameter specifies the video ID for the resource
     * that is being updated.
     * @param defaultLanguage The language of the video's default metadata
     * @param language The language of the localized metadata
     * @param title The localized title to be set
     * @param description The localized description to be set
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun setVideoLocalization(videoId: String, defaultLanguage: String,
                                     language: String, title: String, description: String) {
        // Call the YouTube Data API's videos.list method to retrieve videos.
        val videoListResponse = youtube!!.videos().list("snippet,localizations").setId(videoId).execute()

        // Since the API request specified a unique video ID, the API
        // response should return exactly one video. If the response does
        // not contain a video, then the specified video ID was not found.
        val videoList = videoListResponse.items
        if (videoList.isEmpty()) {
            println("Can't find a video with ID: $videoId")
            return
        }
        val video = videoList[0]

        // Modify video's default language and localizations properties.
        // Ensure that a value is set for the resource's snippet.defaultLanguage property.
        video.snippet.defaultLanguage = defaultLanguage

        // Preserve any localizations already associated with the video. If the
        // video does not have any localizations, create a new array. Append the
        // provided localization to the list of localizations associated with the video.
        var localizations: MutableMap<String, VideoLocalization>? = video.localizations
        if (localizations == null) {
            localizations = ArrayMap()
            video.localizations = localizations
        }
        val videoLocalization = VideoLocalization()
        videoLocalization.title = title
        videoLocalization.description = description
        localizations[language] = videoLocalization

        // Update the video resource by calling the videos.update() method.
        val videoResponse = youtube!!.videos().update("snippet,localizations", video)
                .execute()

        // Print information from the API response.
        println("\n================== Updated Video ==================\n")
        println("  - ID: " + videoResponse.id)
        println("  - Default Language: " + videoResponse.snippet.defaultLanguage)
        println("  - Title(" + language + "): " +
                videoResponse.localizations[language]?.getTitle())
        println("  - Description(" + language + "): " +
                videoResponse.localizations[language]?.getDescription())
        println("\n-------------------------------------------------------------\n")
    }

    /**
     * Returns localized metadata for a video in a selected language.
     * If the localized text is not available in the requested language,
     * this method will return text in the default language.
     *
     * @param videoId The id parameter specifies the video ID for the resource
     * that is being updated.
     * @param language The language of the localized metadata
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getVideoLocalization(videoId: String, language: String) {
        // Call the YouTube Data API's videos.list method to retrieve videos.
        val videoListResponse = youtube!!.videos().list("snippet").setId(videoId).set("hl", language).execute()

        // Since the API request specified a unique video ID, the API
        // response should return exactly one video. If the response does
        // not contain a video, then the specified video ID was not found.
        val videoList = videoListResponse.items
        if (videoList.isEmpty()) {
            println("Can't find a video with ID: $videoId")
            return
        }
        val video = videoList[0]

        // Print information from the API response.
        println("\n================== Video ==================\n")
        println("  - ID: " + video.id)
        println("  - Title(" + language + "): " +
                video.localizations[language]?.getTitle())
        println("  - Description(" + language + "): " +
                video.localizations[language]?.getDescription())
        println("\n-------------------------------------------------------------\n")
    }

    /**
     * Returns a list of localized metadata for a video.
     *
     * @param videoId The id parameter specifies the video ID for the resource
     * that is being updated.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun listVideoLocalizations(videoId: String) {
        // Call the YouTube Data API's videos.list method to retrieve videos.
        val videoListResponse = youtube!!.videos().list("snippet,localizations").setId(videoId).execute()

        // Since the API request specified a unique video ID, the API
        // response should return exactly one video. If the response does
        // not contain a video, then the specified video ID was not found.
        val videoList = videoListResponse.items
        if (videoList.isEmpty()) {
            println("Can't find a video with ID: $videoId")
            return
        }
        val video = videoList[0]
        val localizations = video.localizations

        // Print information from the API response.
        println("\n================== Video ==================\n")
        println("  - ID: " + video.id)
        for (language in localizations.keys) {
            println("  - Title(" + language + "): " +
                    localizations[language]?.getTitle())
            println("  - Description(" + language + "): " +
                    localizations[language]?.getDescription())
        }
        println("\n-------------------------------------------------------------\n")
    }

    /*
     * Prompt the user to enter a resource ID. Then return the ID.
     */
    @Throws(IOException::class)
    private fun getId(resource: String): String {

        var id = ""

        print("Please enter a $resource id: ")
        val bReader = BufferedReader(InputStreamReader(System.`in`))
        id = bReader.readLine()

        println("You chose $id for localizations.")
        return id
    }

    /*
     * Prompt the user to enter the localized metadata. Then return the metadata.
     */
    @Throws(IOException::class)
    private fun getMetadata(type: String): String {

        var metadata = ""

        print("Please enter a localized $type: ")
        val bReader = BufferedReader(InputStreamReader(System.`in`))
        metadata = bReader.readLine()

        if (metadata.length < 1) {
            // If nothing is entered, defaults to type.
            metadata = "$type(localized)"
        }

        println("You chose $metadata as localized $type.")
        return metadata
    }

    enum class Action {
        SET,
        GET,
        LIST
    }
}
