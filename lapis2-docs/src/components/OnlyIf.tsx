import type { ReactNode } from 'react';

type Props = {
    children: ReactNode;
    condition: boolean;
};

export const OnlyIf = ({ children, condition }: Props) => {
    return condition ? children : <div>The feature is not available on this instance.</div>;
};
