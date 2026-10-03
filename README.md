<h1 align="center">💫 Starfait 💫</h1>

Starfait is a re-implementation of GameMaker: Studio's runner (YoYo Runner) in Kotlin, targeting Undertale v1.08 (WAD Version 16).

Starfait only targets Undertale v1.08 and NOTHING ELSE. I may add support for DELTARUNE later if I'm feeling fancy.

## Why Kotlin?

When creating Starfait, I thought about some language choices...

* C: Believe it or not C is one of the best languages on this list! The reason I didn't want to go with C is that C's simplicity is also its downfall when talking about *productivity*. It is fun to reinvent the wheel from first principles when implementing things like ArrayLists, or how to manage memory manually, but sometimes you just want to get things *done*, and due to that I feel like Kotlin makes me more productive. But of course, you trade performance for productivity, [but it's a sacrifice that I'm willing to make](https://youtu.be/hiKuxfcSrEU).
* C++: Before I used C++ I thought that C++ was like Java but lower level™. Ended up finding out that C++ feels *worse* to use than *\*gasp\** C somehow? It didn't click with me, even though I'm a OOP enjoyer.
* Rust: I feel like Rust's syntax is too unwieldy for this project. Yes, the unwieldy syntax is like that so that the compiler can find bugs during compilation instead of the application exploding it in your face. You productivity with compile time safetyness, which for a project like this I prefer the productivity. Of course, some people may think that "with LLMs this doesn't matter because they will write everything for you!"... well, I still like writing code. :)
* Go: I don't like Go's error handling, and even though Go compiles to native code, it has around the same performance as the JVM, so it doesn't matter.

So I've decided to go with Kotlin instead. :3

## But... console homebrew!!

[Butterscotch already exists for that use case](https://github.com/ButterscotchRunner/Butterscotch).

## Then why not Butterscotch?

Because I wanted to understand better how the GameMaker VM worked, and to do that I wanted to write a project from scratch to do the coding for me. What I found out is that you only *truly* understand the code if you actually write it yourself, even [if you review all changes that the LLM made](https://x.com/DabsMalone/status/2105300996716957837).

Don't get me wrong, I do think that these tools help speed up development a LOT and I do use them, but you need to have a balance between "coding everything by hand" and "let's just make the LLM write everything for me".

Starfait is not "Butterscotch: Kotlin Edition", Starfait's implementation diverges from Butterscotch in various ways. Because when you research and understand what you want to do, you figure out better ways of implementing things, whereas relying solely on a LLM without understanding the concepts you end up just accepting whatever the LLM outputs, because what you will do? If you don't understand it, you don't know if the output is actually good!

Because Starfait's main star is the VM, the runner part is more sloppy than the VM, that is, some parts of the runner are implemented with LLMs because most of the runner work is essentially "read GameMaker-HTML5's source code and translate it to Kotlin" which is Not Fun™ for me. The VM is not something that's publicly available from YoYo Games so understanding it how it works is important to me, meanwhile if you want to understand how the GameMaker runner works or how specific built-ins are implemented, you can see how GameMaker-HTML5 works, differences between the HTML5 to the C++ runners notwithstanding.

## Why not support ALL games?

Supporting all games is *hard* because GameMaker has TONS of functions. A GameMaker Runner is more akin to Wine or a high level emulator than to a proper console emulator.

So to keep the project simple, I prefer to only support the GameMaker games that I do care about. And the only GameMaker games that I care about are Undertale, DELTARUNE, and my own games that I wrote years ago.