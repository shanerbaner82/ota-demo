<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <title>Laravel</title>

        <link rel="icon" href="/favicon.ico" sizes="any">
        <link rel="icon" href="/favicon.svg" type="image/svg+xml">
        <link rel="apple-touch-icon" href="/apple-touch-icon.png">

        <!-- Fonts -->
        <link rel="preconnect" href="https://fonts.bunny.net">
        <link href="https://fonts.bunny.net/css?family=instrument-sans:400,500,600" rel="stylesheet" />

       @vite('resources/css/app.css')
        @fluxAppearance
    </head>
    <body class="bg-[#FDFDFC] dark:bg-[#0a0a0a] text-[#1b1b18] dark:text-white flex p-6 lg:p-8 items-center lg:justify-center min-h-screen flex-col">
        <p class="text-2xl">Hello Dude!</p>
        <flux:profile avatar="https://unavatar.io/x/calebporzio" />
        <flux:badge color="zinc">Zinc</flux:badge>
        <flux:badge color="red">Red</flux:badge>
        <flux:badge color="red">Red</flux:badge>
        <flux:badge color="red">Red</flux:badge>
        <flux:badge color="red">Red</flux:badge>
        <flux:badge color="red">Red</flux:badge>
    @fluxScripts
    </body>
</html>
