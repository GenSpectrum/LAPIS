import { useEffect, useState } from 'react';

export const ReactCounter = () => {
    const [counter, setCounter] = useState(0);

    useEffect(() => {
        setInterval(() => setCounter((prev) => prev + 1), 1000);
    }, []);

    return <>Counter: {counter}</>;
};
