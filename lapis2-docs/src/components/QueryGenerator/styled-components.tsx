import type { InputHTMLAttributes, ReactNode } from 'react';

type WrapperProps = {
    children?: ReactNode;
};

export const ContainerWrapper = ({ children }: WrapperProps) => <div className='flex flex-col gap-4'>{children}</div>;

export const LabelWrapper = ({ children }: WrapperProps) => <div className='mb-2 font-medium'>{children}</div>;

export const CheckBoxesWrapper = ({ children }: WrapperProps) => <div className='flex flex-wrap'>{children}</div>;

type LabeledCheckBoxProps = {
    label: string;
} & InputHTMLAttributes<HTMLInputElement>;

export const LabeledCheckBox = ({ label, className, ...props }: LabeledCheckBoxProps) => (
    <label className={`flex gap-2 items-center ${className}`}>
        <input {...props} className='checkbox checkbox-sm border border-solid' />
        {label}
    </label>
);
