// randomTextOnlyEverFailsToParse parses 40,000 strings: 0.8 s in a desktop browser, and past
// Mocha's 2 second default on a CI runner compiling colorpicker's tests beside it. A smaller
// sample reaches fewer of the parser's error paths, so the limit moves rather than the sample.
config.set({
    client: {
        mocha: {
            timeout: 120000,
        },
    },
});
