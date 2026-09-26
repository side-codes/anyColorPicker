// The plane budget tests sweep millions of conversions to measure where a rasterized
// surface drifts from the true one, which takes longer than Mocha's 2 second default on
// the browser runner. That density is what makes the budgets mean anything — a coarser
// grid steps over the needle the error peaks in — so the limit moves rather than the
// sampling.
config.set({
    client: {
        mocha: {
            timeout: 120000,
        },
    },
});
