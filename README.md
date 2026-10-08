<h1 align="center">💫 Starfait 💫</h1>

<p align="center">
    <img src="https://badgen.now.sh/badge/asriel/fluffy/blue">
</p>

Starfait is a re-implementation of GameMaker: Studio's runner (YoYo Runner) in Kotlin, targeting Undertale v1.08 (WAD Version 16).

Starfait only targets Undertale v1.08 and NOTHING ELSE. I may add support for DELTARUNE later if I'm feeling fancy.

## Why not support ALL games?

Supporting all games is *hard* because GameMaker has TONS of builtin functions and features. A GameMaker Runner is more akin to Wine or a high-level emulator than to a console emulator.

To keep the project's scope simple, I only plan to support the GameMaker games that I do care about. And the only GameMaker games that I care about are Undertale, DELTARUNE, and my own games that I wrote years ago.

## Notes and Write-Ups

Things that I learned along the way while developing Starfait.

* [Bytecode](docs/bytecode.md)
* [Precise Collision Masks](docs/precise-collision-masks.md)
* [Quirks and Miscellaneous Things](docs/quirks.md)

## Why Kotlin?

Because I love Kotlin. <img src="docs/images/kodee-loving.png" width="32">

*Something something but the JVM is slow!!* [I don't give a damn](https://youtu.be/lhCPiauHI-g?t=103)

## But... console homebrew!! I want Undertale on my *insert console here*!!!

[Butterscotch already exists for that use case](https://github.com/ButterscotchRunner/Butterscotch).

If you want to see a what-if scenario, [here's what Starfait would look like if it was written in C](https://github.com/MrPowerGamerBR/StarfaitC).

## Why "Starfait"?

[Butterscotch](https://github.com/ButterscotchRunner/Butterscotch) was already taken, [Cinnamon](https://github.com/Project-Sunshine-Native/cinnamon) was already taken, so why not [Starfait](https://undertale.wiki/w/Starfait)?

## Acknowledgements

* [GameMaker-HTML5](https://github.com/YoYoGames/GameMaker-HTML5) - GameMaker's HTML5 runner, has references of how the runner logic works and has builtins implementations.
* [UndertaleModTool](https://github.com/UnderminersTeam/UndertaleModTool) - Very useful to check what Undertale's decompiled code looks like compared to what the bytecode looks like.
* [Ghidra](https://github.com/nationalsecurityagency/ghidra) - For decompiling the YoYo Runner's code for references.
* Toby Fox - For creating Undertale and DELTARUNE!