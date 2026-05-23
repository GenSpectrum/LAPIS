import { type FC, type PropsWithChildren, type ReactNode } from 'react';

/**
 * One step's heading. Step name comes from the stepper above; this is
 * a sub-section heading inside the step body.
 */
export const SectionHeading: FC<PropsWithChildren> = ({ children }) => (
    <h2 className='text-base font-semibold text-base-content/90'>{children}</h2>
);

/**
 * One labelled input. Label sits above the control. `help` renders
 * inline with the label text (typically a <HelpTooltip>). `description`
 * renders below the control as small grey text. Setting `required`
 * adds a visual asterisk after the label.
 */
export const Field: FC<{
    label: string;
    help?: ReactNode;
    description?: ReactNode;
    required?: boolean;
    children: ReactNode;
}> = ({ label, help, description, required, children }) => (
    <div className='flex flex-col gap-1'>
        <label className='text-sm font-medium flex items-center'>
            {label}
            {required === true && (
                <span className='text-error ml-0.5' aria-hidden='true'>
                    *
                </span>
            )}
            {help}
        </label>
        {children}
        {description !== undefined && <span className='text-xs text-base-content/60'>{description}</span>}
    </div>
);

/**
 * Horizontal divider between subsections within a step.
 */
export const SectionDivider: FC = () => <hr className='border-base-300' />;
