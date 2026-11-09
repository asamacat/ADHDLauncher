# ADHD Launcher Dictionary & Localization Guide

This directory (`app/src/main/assets/dictionaries/`) contains externalized JSON rules for **App Auto-Categorization** and **Incremental Search Aliases**.

No rules or keywords are hardcoded in Kotlin source code. Overseas contributors can easily add support for their language or regional apps by editing `app_dictionary.json` (or adding locale-specific files like `app_dictionary_en.json`, `app_dictionary_ko.json`) and submitting a Pull Request on GitHub.

## Structure of `app_dictionary.json`

1. **`package_rules`**: Exact Android `packageName` mappings to one of the categories:
   - `COMMUNICATION`
   - `FINANCE`
   - `INTERNET`
   - `MEDIA`
   - `GAMES`
   - `TOOLS`
2. **`category_keywords`**: Substring keywords matched against the lowercase `packageName` or localized app `label`.
3. **`search_aliases`**: Localized phonetic/romanized readings or synonyms mapped from app label substrings so users can find apps with 1-2 keystrokes in any script.
