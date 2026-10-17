# WinPack/TranslaTale WADs

Old Undertale translations were made with [TranslaTale](https://github.com/AlexWaveDiver/TranslaTale), a tool that uses [WinPack](https://github.com/AlexWaveDiver/UndertaleTools) to mod the game's WAD files.

The issue with WinPack is that it creates WAD files that are inherently "borky", that is, the original YoYo compiler does not generate WADs like that, *but* the original YoYo Runner accepts it anyway.

Here are some of the quirks that WinPack/TranslaTale WADs have:

1. The length of strings in the `STRG` chunk is not correct/updated. The original runner doesn't care about it because it reads up until instead of relying on the string length.
2. String pointers in chunks can point to data outside the `STRG` chunk.
3. Likewise, texture page entries can also point to data outside the `TPAG` chunk.
4. For the injected texture page entries, the texture index is always `+ 1` than what it should be, that's because it uses the runner's *runtime* texture index, which is always `+ 1` because the first texture in the original YoYo Runner is always a white 1x1 texture.