type Props = {
    label: string;
    value: string | number | undefined;
    onChange: (value: string) => void;
};

export const LabelledInput = ({ label, value, onChange }: Props) => {
    return (
        <div>
            <label className='mr-2'>{label}</label>
            <input
                type='text'
                className='input input-bordered w-48'
                value={value}
                onChange={(e) => onChange(e.target.value)}
            />
        </div>
    );
};
