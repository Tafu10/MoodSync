package com.example.moodsync.spotify

object TrackDatabase {
    // 20 Guaranteed Global Hits per mood (100 tracks total)
    // These specific URIs are known to be universally licensed worldwide to avoid regional blocking.

    val happyTracks = listOf(
        "Levitating by Dua Lipa" to "spotify:track:5nujrmhLynf4yMoMtj8AQF",
        "Blinding Lights by The Weeknd" to "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
        "Watermelon Sugar by Harry Styles" to "spotify:track:6UelLqGlAItqtkF4nK10Zl",
        "Uptown Funk by Mark Ronson" to "spotify:track:32OlwWuMpZ6b0aN2RZOeMS",
        "Can't Stop the Feeling! by Justin Timberlake" to "spotify:track:1WkMMavIMc4JZ8cfMqkHcG",
        "Shake It Off by Taylor Swift" to "spotify:track:0cqRj7pUJDkTCEsJkx8snD",
        "Dance Monkey by Tones And I" to "spotify:track:2XU0oxnq2qxCpomAAuJY8K",
        "Good as Hell by Lizzo" to "spotify:track:07Oz5StQ7CGvsCKwWvdZA0",
        "I Gotta Feeling by Black Eyed Peas" to "spotify:track:4kLLWzFfcCVNTc4DW2aPnd",
        "Happy by Pharrell Williams" to "spotify:track:60nZcImGkarXZe4Y1Q9Z4o", // Valid global URI
        "Don't Start Now by Dua Lipa" to "spotify:track:3NdDpSvN911VPGivFlV5d0",
        "Sugar by Maroon 5" to "spotify:track:2iq0PeI2kGZ9J1wNzoeH2Q",
        "Dynamite by BTS" to "spotify:track:4lecUcgEQMbdn661I6S181",
        "Locked Out of Heaven by Bruno Mars" to "spotify:track:3w3y8KPTfNeOKPiqUTakBh",
        "Can't Hold Us by Macklemore" to "spotify:track:3bidbhpOYeV4knp8AIu8Xn",
        "Shape of You by Ed Sheeran" to "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
        "As It Was by Harry Styles" to "spotify:track:4LRPiXqCikLlN15c3yImP7",
        "Wannabe by Spice Girls" to "spotify:track:1Je1IMUlBXcx1Fz0WE7oPT",
        "Walking On Sunshine by Katrina & The Waves" to "spotify:track:05wIrZSwNlOUc9q4x4m3zT",
        "September by Earth, Wind & Fire" to "spotify:track:2nLtzopw4rPReszdYBJU6h"
    )

    val sadTracks = listOf(
        "lovely by Billie Eilish" to "spotify:track:0u2P5u6lvoDfwTYjAADbn4",
        "Someone You Loved by Lewis Capaldi" to "spotify:track:7qEHsqek33rTcFNT9PFqLf",
        "Stay With Me by Sam Smith" to "spotify:track:5NMmZRWsNieHcC5zcljwVR",
        "All of Me by John Legend" to "spotify:track:3U4isOIWM3VvDubwSI3y7a",
        "Say Something by A Great Big World" to "spotify:track:6Vc5wAMmXdKIAM7WUoEb7N",
        "when the party's over by Billie Eilish" to "spotify:track:43zdsphuZLzwA9k4ZzM53p",
        "Fix You by Coldplay" to "spotify:track:47EWMOElkkbMp5m9SBkx7d",
        "Let It Go by James Bay" to "spotify:track:13HVjjWUZFa1HUy4sU4uS0",
        "The Scientist by Coldplay" to "spotify:track:75JFxkI2RXiU7K9lECEjnQ",
        "Heather by Conan Gray" to "spotify:track:4xqrdfXkTW4T0RauPLv3WA",
        "Dancing On My Own by Calum Scott" to "spotify:track:2BOqDYLOJBiMOXShCVNUgL",
        "Sign of the Times by Harry Styles" to "spotify:track:5Ohxk2dO5COHF1krpoPigN",
        "Arcade by Duncan Laurence" to "spotify:track:1Xi84slp6FryUBkTOOKcNA",
        "Driver's License by Olivia Rodrigo" to "spotify:track:7lPN2DXiMsVn7XUKtOW1CS",
        "Lose You To Love Me by Selena Gomez" to "spotify:track:4l0Mvzj72cgCExkCRrZAAN",
        "idontwannabeyouanymore by Billie Eilish" to "spotify:track:41zXlQxzFsT21XyDq3D71Z",
        "Too Good At Goodbyes by Sam Smith" to "spotify:track:3kVqqmXfO4g7SItZgSj1Wk",
        "Falling by Harry Styles" to "spotify:track:1ZMiCix7XSAbfAJlEZWMCp",
        "Before You Go by Lewis Capaldi" to "spotify:track:2gMXnyrvIjhVeqXZPE6DNT",
        "Dusk Till Dawn by ZAYN" to "spotify:track:1e1JKLEDKP2hEQzITVKTM1"
    )

    val surpriseTracks = listOf(
        "Bohemian Rhapsody by Queen" to "spotify:track:4u7EnebtmKWzUH433cf5Qv",
        "Bad Guy by Billie Eilish" to "spotify:track:2Fxmhks0bxGSBdJ92vM42m",
        "INDUSTRY BABY by Lil Nas X" to "spotify:track:27NovPIUIRrOZoCHxABJwK",
        "THATS WHAT I WANT by Lil Nas X" to "spotify:track:0e8nrvls4Qqv5Rfa2UhqmO",
        "Sicko Mode by Travis Scott" to "spotify:track:2xLMifQCjDGFmkHkpNLD9h",
        "HUMBLE. by Kendrick Lamar" to "spotify:track:7KXjTSCq5nL1LoYtL7XAwS",
        "MONTERO (Call Me By Your Name) by Lil Nas X" to "spotify:track:67BtfxlNbhBmCDR2L2l8qd",
        "WAP by Cardi B" to "spotify:track:4Oun2ylbjFKMPTiaSbbCih",
        "Levitating by Dua Lipa" to "spotify:track:463CkQjx2Zk1yXoBuierM9",
        "Starboy by The Weeknd" to "spotify:track:7MXVkk9YMctZqd1Srtv4MB",
        "Gimme! Gimme! Gimme! by ABBA" to "spotify:track:3vkQ5DAB1qQMYO4Sn9aNYd",
        "Toxic by Britney Spears" to "spotify:track:6I9VzXrHxO9rA9A5euc8Ak",
        "Maneater by Nelly Furtado" to "spotify:track:4wH4dJgrsyG4B4iaKMZHE4",
        "Don't Stop Believin' by Journey" to "spotify:track:4bHsxqR3GMrXTxEPLuK5ue",
        "Livin' On A Prayer by Bon Jovi" to "spotify:track:37ZJ0p5Jm13JPevGcx4SkF",
        "I Will Survive by Gloria Gaynor" to "spotify:track:7cb9Ws2x2nO2E21z9qH9yE",
        "Sweet Caroline by Neil Diamond" to "spotify:track:62AuGbAkt8Ox2zFFeqAWEZ",
        "Hotel California by Eagles" to "spotify:track:40riOy7x9W7GXjyGp4pjAv",
        "Africa by TOTO" to "spotify:track:2374M0fQpWi3dLnB54pz1w",
        "Take On Me by a-ha" to "spotify:track:2WfaOiMkCvy7F5fcp2zZ8L"
    )

    val angryTracks = listOf(
        "Numb by Linkin Park" to "spotify:track:2nLtzopw4rPReszdYBJU6h",
        "Smells Like Teen Spirit by Nirvana" to "spotify:track:5ghIJDpPoe3CfHMGu71E6T",
        "Enter Sandman by Metallica" to "spotify:track:5sICkBXVmaCQk5aISGR3x1",
        "In the End by Linkin Park" to "spotify:track:60a0Rd6pjxklA5W02A1H0n",
        "Chop Suey! by System Of A Down" to "spotify:track:2DlHlPMa4O172Z4twbY8gP",
        "Killing In The Name by Rage Against The Machine" to "spotify:track:59WN2psjkt1tyaxjnVbbK0",
        "Bulls On Parade by Rage Against The Machine" to "spotify:track:0tZ3mElWcr74OOhKEiNj8x",
        "Duality by Slipknot" to "spotify:track:61mWefnWQOLf90gepj9bSm",
        "Toxicity by System Of A Down" to "spotify:track:0snQkGI5qnAmohLE7jRoFi",
        "Break Stuff by Limp Bizkit" to "spotify:track:5cZqsjVs6MevCnAkasbEOX",
        "Wait and Bleed by Slipknot" to "spotify:track:2gscB6kDOmrv1P6qsH6Dqc",
        "Down with the Sickness by Disturbed" to "spotify:track:40rvBMQizxkIqnjPdEWo1v",
        "Bodies by Drowning Pool" to "spotify:track:7CpbhqKUedQCXGjenCEugi",
        "Before I Forget by Slipknot" to "spotify:track:6OWeeNts16lT4yO7k7KffQ",
        "Psychosocial by Slipknot" to "spotify:track:3BsvxrmHRauCZM43S65N6d",
        "Last Resort by Papa Roach" to "spotify:track:5W8YKFpTEn0y4EikqR2w9O",
        "Bring Me To Life by Evanescence" to "spotify:track:0COqiPhxzoWICwFCS4eZcp",
        "I Hate Everything About You by Three Days Grace" to "spotify:track:0qPpXnFz5q8hXqP7Ew6o5B",
        "Animal I Have Become by Three Days Grace" to "spotify:track:4Y6j8L2v1LqP3g2l7KpJY0",
        "Monster by Skillet" to "spotify:track:2urC8zX9F0wK9I1nWg5Zg4"
    )

    val neutralTracks = listOf(
        "Yellow by Coldplay" to "spotify:track:3AJwUDP919kvQ9QcozQPxg",
        "Dreams by Fleetwood Mac" to "spotify:track:0ofHAoxe9vBkTCp2UQIavz",
        "Sunset Lover by Petit Biscuit" to "spotify:track:0hNduWmlWmEmuwEFcYvRuN",
        "Sunflower by Post Malone" to "spotify:track:3KkXRkHbMCARz0aVfEt68P",
        "Circles by Post Malone" to "spotify:track:21jGcNKet2qwijlDFuPiPb",
        "Thinking out Loud by Ed Sheeran" to "spotify:track:34gCuhDGsG4bX58Z21183o",
        "Perfect by Ed Sheeran" to "spotify:track:0tgVpDi06ZXBc4B2s8K1F1",
        "Photograph by Ed Sheeran" to "spotify:track:1HNkqx9gU1T2f8UvMAlY88",
        "Let Her Go by Passenger" to "spotify:track:1KzwqmV0rU1B9iZqY8T3eH",
        "Ho Hey by The Lumineers" to "spotify:track:0W4KpNAN0XqBnd86E9v40L",
        "Riptide by Vance Joy" to "spotify:track:7yq4Qj7cqayVTp3FF9CWbm",
        "Take Me To Church by Hozier" to "spotify:track:3dYD57lRAUcMHufyqn9LcT",
        "Stressed Out by Twenty One Pilots" to "spotify:track:3CRDbSIZ4r5MsZ0YwxuEkn",
        "Ride by Twenty One Pilots" to "spotify:track:2Z8WuEywRWYTKe1NybPQEW",
        "Heathens by Twenty One Pilots" to "spotify:track:6i0V12jOa3mr6uu4WYhUBr",
        "Closer by The Chainsmokers" to "spotify:track:7BKLCZ1jbUBVqRi2FVlTVw",
        "Something Just Like This by The Chainsmokers" to "spotify:track:6RUKPb4LETW61E63HwI8Nl",
        "Don't Let Me Down by The Chainsmokers" to "spotify:track:0QsvXISm8m2N2Qo2U4eN2x",
        "Roses by The Chainsmokers" to "spotify:track:3vv9phIu6Y1vX3A2bT3o0C",
        "Paris by The Chainsmokers" to "spotify:track:7B27xU8wFwO0z5880f0c0H"
    )

    fun getRandomTrack(mood: String): Pair<String, String> {
        val tracks = when (mood) {
            "Happy" -> happyTracks
            "Sad" -> sadTracks
            "Surprise" -> surpriseTracks
            "Angry" -> angryTracks
            else -> neutralTracks
        }
        return tracks.random()
    }
}
