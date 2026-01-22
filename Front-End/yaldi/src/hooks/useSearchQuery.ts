import { useSearchParams } from "react-router-dom"; 


// url의 query parameter 'query'를 관리하는 custom hook
export const useSearchQuery = () => {
    const [searchQuery, setSearchQuery] = useSearchParams();
    const query = searchQuery.get("query") || "";

    const setQuery = (newQuery: string) => {
        if (newQuery) {
            setSearchQuery({ query: newQuery }, { replace: true });
        } else {
            searchQuery.delete("query");
            setSearchQuery(searchQuery, { replace: true });
        }
    };

    return { query, setQuery } as const;
};