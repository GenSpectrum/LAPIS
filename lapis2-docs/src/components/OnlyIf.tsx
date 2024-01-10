import type { PropsWithChildren } from 'react';

type Props = PropsWithChildren<{
    condition: boolean;
}>;

export const OnlyIf = ({ children, condition }: Props) => {
    return condition && children;
};
