const getRoutes = (prefix: string) => ({
    DRAFT: {
        MAIN_PAGE: {
            PATH: '/',
        },
        ADMIN_PANEL: {
            PATH: '/adminPanel',
        },
        PAGE_WITH_ARTICLES: {
            PATH: '/articles',
        },
        PUBLIC_STRATEGY: {
            PATH: '/publicStrategy',
        },
        EQUITY:{
            PATH:'/equity',
        }
    },
});
let x = getRoutes("");
export const ROOT = (): ReturnType<typeof getRoutes> => x;