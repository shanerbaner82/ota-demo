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
        <flux:button>Test</flux:button>
        <flux:dropdown>
            <flux:button
                icon="adjustments-horizontal"
                icon:variant="micro"
                icon:class="text-zinc-400"
                icon-trailing="chevron-down"
                icon-trailing:variant="micro"
                icon-trailing:class="text-zinc-400"
            >
                Options
            </flux:button>

            <flux:popover class="flex flex-col gap-4">
                <flux:radio.group wire:model="sort" label="Sort by" label:class="text-zinc-500 dark:text-zinc-400">
                    <flux:radio value="active" label="Recently active" />
                    <flux:radio value="posted" label="Date posted" checked />
                </flux:radio.group>

                <flux:separator variant="subtle" />

                <flux:radio.group wire:model="view" label="View as" label:class="text-zinc-500 dark:text-zinc-400">
                    <flux:radio value="list" label="List" checked />
                    <flux:radio value="gallery" label="Gallery" />
                </flux:radio.group>

                <flux:separator variant="subtle" />

                <flux:button variant="subtle" size="sm" class="justify-start -m-2 px-2!">Reset to default</flux:button>
            </flux:popover>
        </flux:dropdown>
    @fluxScripts
    </body>
</html>
