package com.example.adhdlauncher.data

object JapaneseSearchMatcher {

    // ローマ字 -> ひらがな変換テーブル（3文字・2文字・1文字の順で最長一致変換）
    private val romajiToHiragana = listOf(
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
        "shi" to "し", "chi" to "ち", "tsu" to "つ",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "ti" to "ち", "tu" to "つ", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を", "nn" to "ん", "n'" to "ん",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "zi" to "じ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お"
    )

    private val sokuonConsonants = setOf(
        'k', 's', 't', 'p', 'c', 'g', 'z', 'j', 'd', 'b', 'f', 'h', 'm', 'r', 'w', 'y'
    )

    private fun katakanaToHiragana(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '\u30A1'..'\u30F6') {
                sb.append((ch.code - 0x60).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun romajiToKana(input: String): String {
        val lower = input.lowercase()
        // 1. 促音（っ）の変換: 子音連続（kk, ss, tt, pp 等、ただし nn は除く）
        val sokuonProcessed = StringBuilder(lower.length)
        var i = 0
        while (i < lower.length) {
            val c = lower[i]
            if (i + 1 < lower.length && c == lower[i + 1] && c in sokuonConsonants) {
                sokuonProcessed.append('っ')
                i++
            } else {
                sokuonProcessed.append(c)
                i++
            }
        }

        // 2. ローマ字音節をひらがなへ置換
        var s = sokuonProcessed.toString()
        for ((rom, hira) in romajiToHiragana) {
            s = s.replace(rom, hira)
        }

        // 3. 撥音（ん）の変換: 母音や y 以外の直前・または末尾に残った単独の 'n' を 'ん' に変換
        val finalSb = StringBuilder(s.length)
        for (idx in s.indices) {
            val ch = s[idx]
            if (ch == 'n') {
                val next = if (idx + 1 < s.length) s[idx + 1] else null
                if (next == null || next !in setOf('a', 'i', 'u', 'e', 'o', 'y')) {
                    finalSb.append('ん')
                } else {
                    finalSb.append(ch)
                }
            } else {
                finalSb.append(ch)
            }
        }
        return finalSb.toString()
    }

    private fun buildSearchableIndex(app: AppInfo): String {
        val base = "${app.label.lowercase()} ${app.packageName.lowercase()}"
        val hiraLabel = katakanaToHiragana(app.label.lowercase())
        val extra = StringBuilder()
        // 外部 JSON (assets/dictionaries/app_dictionary.json) から読み込んだエイリアス辞書を参照
        for ((keyword, alias) in AppCategoryClassifier.getSearchAliases()) {
            if (app.label.contains(keyword, ignoreCase = true)) {
                extra.append(' ').append(alias.lowercase())
            }
        }
        return "$base $hiraLabel $extra"
    }

    fun matches(app: AppInfo, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true

        val index = buildSearchableIndex(app)
        if (index.contains(q)) return true

        val qHira = katakanaToHiragana(q)
        if (index.contains(qHira)) return true

        val qRomajiHira = romajiToKana(q)
        if (qRomajiHira.isNotEmpty() && index.contains(qRomajiHira)) return true

        return false
    }
}
